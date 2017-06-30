package scalafix
package cli

import java.io.File
import java.io.OutputStreamWriter
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.internal.inputs._
import scala.meta.internal.io.PlatformFileIO
import scala.meta.internal.semantic.vfs
import scala.meta.io.AbsolutePath
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.config.Class2Hocon
import scalafix.config.FilterMatcher
import scalafix.config.LazyMirror
import scalafix.config.MetaconfigPendingUpstream
import scalafix.config.PrintStreamReporter
import scalafix.config.RewriteKind
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.internal.cli.CommonOptions
import scalafix.internal.cli.FixFile
import scalafix.internal.cli.ScalafixOptions
import scalafix.internal.cli.TermDisplay
import scalafix.internal.cli.WriteMode
import scalafix.reflect.ScalafixReflect
import scalafix.syntax._
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import metaconfig._

sealed abstract case class CliRunner(
    sourceroot: AbsolutePath,
    cli: ScalafixOptions,
    config: ScalafixConfig,
    rewrite: Rewrite,
    inputs: Seq[FixFile],
    replacePath: AbsolutePath => AbsolutePath
) {
  val sbtConfig: ScalafixConfig = config.copy(dialect = dialects.Sbt0137)
  val writeMode: WriteMode =
    if (cli.stdout) WriteMode.Stdout
    else WriteMode.WriteFile
  val common: CommonOptions = cli.common
  implicit val workingDirectory: AbsolutePath = common.workingPath

  def run(): ExitStatus = {
    val display = new TermDisplay(new OutputStreamWriter(System.out))
    if (inputs.length > 10) display.init()
    if (inputs.isEmpty) common.err.println("Running scalafix on 0 files.")
    val msg = s"Running scalafix rewrite ${rewrite.name}..."
    display.startTask(msg, common.workingDirectoryFile)
    display.taskLength(msg, inputs.length, 0)
    val exitCode = new AtomicReference(ExitStatus.Ok)
    val counter = new AtomicInteger()
    val inputsToFix =
      if (cli.singleThread) inputs
      else inputs.toVector.par
    inputsToFix.foreach { input =>
      val code = safeHandleInput(input)
      val progress = counter.incrementAndGet()
      exitCode.getAndUpdate(new UnaryOperator[ExitStatus] {
        override def apply(t: ExitStatus): ExitStatus =
          ExitStatus.merge(code, t)
      })
      display.taskProgress(msg, progress)
      code
    }
    display.stop()
    exitCode.get()
  }

  // safeguard to verify that the original file contents have not changed since the
  // creation of the .semanticdb file. Without this check, we risk overwriting
  // the latest changes written by the user.
  private def isUpToDate(input: FixFile): Boolean =
    if (!input.toIO.exists() && cli.outTo.nonEmpty) true
    else {
      input.mirror match {
        case Some(Input.LabeledString(_, contents)) =>
          val fileToWrite = scala.io.Source.fromFile(input.toIO)
          try fileToWrite.sameElements(contents.toCharArray.toIterator)
          finally fileToWrite.close()
        case _ =>
          true // No way to check for Input.File, see https://github.com/scalameta/scalameta/issues/886
      }
    }

  def safeHandleInput(input: FixFile): ExitStatus = {
    try {
      val inputConfig = if (input.original.isSbtFile) sbtConfig else config
      inputConfig.dialect(input.toParse).parse[Source] match {
        case parsers.Parsed.Error(pos, message, _) =>
          if (cli.quietParseErrors && !input.passedExplicitly) {
            // Ignore parse errors, useful for example when running scalafix on
            // millions of lines of code for experimentation.
            ExitStatus.Ok
          } else {
            common.err.println(pos.formatMessage("error", message))
            ExitStatus.ParseError
          }
        case parsers.Parsed.Success(tree) =>
          val ctx = RewriteCtx(tree, config)
          val fixed = rewrite(ctx)
          if (writeMode.isWriteFile) {
            val outFile = replacePath(input.original.path)
            if (isUpToDate(input)) {
              Files.write(outFile.toNIO,
                          fixed.getBytes(input.original.charset))
              ExitStatus.Ok
            } else {
              ctx.reporter.error(
                s"Stale semanticdb for ${CliRunner.pretty(outFile)}, skipping rewrite. Pease recompile.")
              if (cli.verbose) {
                val diff = Patch.unifiedDiff(input.mirror.get, input.original)
                common.err.println(diff)
              }
              ExitStatus.StaleSemanticDB
            }
          } else {
            common.out.write(fixed.getBytes)
            ExitStatus.Ok
          }
      }
    } catch {
      case NonFatal(e) =>
        reportError(input.original.path, e, cli)
        e match {
          case _: scalafix.Failure => ExitStatus.ScalafixError
          case _ => ExitStatus.UnexpectedError
        }
    }
  }

  def reportError(path: AbsolutePath,
                  cause: Throwable,
                  options: ScalafixOptions): Unit = {
    options.common.reporter.error(s"Failed to fix $path")
    cause.setStackTrace(
      cause.getStackTrace.take(options.common.stackVerbosity))
    cause.printStackTrace(options.common.err)
  }
}

object CliRunner {
  private[scalafix] def pretty(path: AbsolutePath)(
      implicit cwd: AbsolutePath): String = {
    // toRelative should not throw exceptions, but it does, see https://github.com/scalameta/scalameta/issues/821
    Try(path.toRelative(cwd)).getOrElse(path).toString
  }

  def fromOptions(options: ScalafixOptions): Configured[CliRunner] = {
    val builder = new CliRunner.Builder(options)
    for {
      rewrite <- builder.resolvedRewrite
      replace <- builder.resolvedPathReplace
      inputs <- builder.fixFilesWithMirror
      config <- builder.resolvedConfig
    } yield {
      if (options.verbose) {
        options.common.err.println(
          s"""|Config:
              |${Class2Hocon(config)}
              |Rewrite:
              |$rewrite
              |""".stripMargin
        )
      }
      new CliRunner(
        sourceroot = builder.resolvedSourceroot,
        cli = options,
        config = config,
        rewrite = rewrite,
        replacePath = replace,
        inputs = inputs
      ) {}
    }
  }
  private val META_INF = Paths.get("META-INF")
  private val SEMANTICDB = Paths.get("semanticdb")

  private def isTargetroot(path: Path): Boolean = {
    path.toFile.isDirectory &&
    path.resolve(META_INF).toFile.isDirectory &&
    path.resolve(META_INF).resolve(SEMANTICDB).toFile.isDirectory
  }
  def autoClasspath(workingPath: AbsolutePath): Classpath = {
    val buffer = List.newBuilder[AbsolutePath]
    Files.walkFileTree(
      workingPath.toNIO,
      new SimpleFileVisitor[Path] {
        override def preVisitDirectory(
            dir: Path,
            attrs: BasicFileAttributes): FileVisitResult = {
          if (isTargetroot(dir)) {
            buffer += AbsolutePath(dir)
            FileVisitResult.SKIP_SUBTREE
          } else {
            FileVisitResult.CONTINUE
          }
        }
      }
    )
    Classpath(buffer.result())
  }

  private class Builder(val cli: ScalafixOptions) {
    import cli._

    implicit val workingDirectory: AbsolutePath = common.workingPath

    def resolveClasspath: Configured[Classpath] =
      classpath match {
        case Some(cp) =>
          val paths = cp.split(File.pathSeparator).map { path =>
            AbsolutePath.fromString(path)(common.workingPath)
          }
          Ok(Classpath(paths))
        case None =>
          val cp = autoClasspath(common.workingPath)
          if (verbose) {
            common.err.println(s"Automatic classpath=$cp")
          }
          if (cp.shallow.nonEmpty) Ok(cp)
          else {
            val msg =
              """Unable to automatically detect .semanticdb files to run semantic rewrites. Possible workarounds:
                |- re-compile sources with the scalahost compiler plugin enabled.
                |- explicitly pass in --classpath and --sourceroot to run semantic rewrites.""".stripMargin
            ConfError.msg(msg).notOk
          }
      }

    val resolvedSourceroot: AbsolutePath =
      sourceroot
        .map(AbsolutePath.fromString(_))
        .getOrElse(common.workingPath)

    // We don't know yet if we need to compute the database or not.
    // If all the rewrites are syntactic, we never need to compute the mirror.
    // If a single rewrite is semantic, then we need to compute the database.
    private var cachedDatabase = Option.empty[Configured[Database]]
    private def hasDatabase = cachedDatabase.isDefined
    private def computeAndCacheDatabase(): Option[Database] = {
      val result: Configured[Database] = cachedDatabase.getOrElse {
        try {
          resolveClasspath.map { classpath =>
            val db = Database.load(classpath, Sourcepath(resolvedSourceroot))
            if (verbose) {
              common.err.println(
                s"Loaded database with ${db.entries.length} entries.")
            }
            db
          }
        } catch {
          case NonFatal(e) =>
            ConfError.exception(e, common.stackVerbosity).notOk
        }
      }
      if (cachedDatabase.isEmpty) {
        cachedDatabase = Some(result)
      }
      result.toEither.right.toOption
    }
    private def resolveDatabase(kind: RewriteKind): Option[Database] = {
      if (kind.isSyntactic) None
      else computeAndCacheDatabase()
    }
    private val lazyMirror: LazyMirror = resolveDatabase

    // expands a single file into a list of files.
    def expand(matcher: FilterMatcher)(path: AbsolutePath): Seq[FixFile] = {
      if (!path.toFile.exists()) {
        common.err.println(s"$path does not exist.")
        Nil
      } else if (path.isDirectory) {
        val builder = Seq.newBuilder[FixFile]
        Files.walkFileTree(
          path.toNIO,
          new SimpleFileVisitor[Path] {
            override def visitFile(
                file: Path,
                attrs: BasicFileAttributes): FileVisitResult = {
              if (Cli.isScalaPath(file) && matcher.matches(file.toString)) {
                builder += FixFile(Input.File(AbsolutePath(file)))
              }
              FileVisitResult.CONTINUE
            }
          }
        )
        builder.result()
      } else if (matcher.matches(path.toString())) {
        // if the user provided explicit path, take it.
        List(FixFile(Input.File(path), passedExplicitly = true))
      } else {
        Nil
      }
    }

    private val resolvedPathMatcher: Configured[FilterMatcher] = try {
      val include = if (files.isEmpty) List(".*") else files
      Ok(FilterMatcher(include, exclude))
    } catch {
      case e: PatternSyntaxException =>
        ConfError
          .msg(
            s"Invalid '${e.getPattern}' for  --include/--exclude. ${e.getMessage}")
          .notOk
    }

    val fixFiles: Configured[Seq[FixFile]] = for {
      pathMatcher <- resolvedPathMatcher
    } yield {
      val paths =
        if (cli.files.nonEmpty) cli.files.map(AbsolutePath.fromString(_))
        // If no files are provided, assume cwd.
        else common.workingPath :: Nil
      paths.toVector.flatMap(expand(pathMatcher))
    }

    val resolvedConfigInput: Configured[Input] =
      (config, configStr) match {
        case (Some(x), Some(y)) =>
          ConfError
            .msg(s"Can't configure both --config $x and --config-str $y")
            .notOk
        case (Some(configPath), _) =>
          val path = AbsolutePath.fromString(configPath)
          if (path.isFile) Ok(Input.File(path))
          else ConfError.msg(s"--config $path is not a file").notOk
        case (_, Some(configString)) =>
          Ok(Input.String(configString))
        case _ =>
          Ok(
            ScalafixConfig
              .auto(common.workingPath)
              .getOrElse(Input.String("")))
      }

    // ScalafixConfig and Rewrite
    // Determines the config in the following order:
    //  - --config-str or --config
    //  - .scalafix.conf in working directory
    //  - ScalafixConfig.default
    val resolvedRewriteAndConfig: Configured[(Rewrite, ScalafixConfig)] = {
      val decoder = ScalafixReflect.fromLazyMirror(lazyMirror)
      for {
        inputs <- fixFiles
        configuration <- {
          if (inputs.isEmpty) Ok(Rewrite.empty -> ScalafixConfig.default)
          else {
            resolvedConfigInput.flatMap(input =>
              ScalafixConfig.fromInput(input, lazyMirror, rewrites)(decoder))
          }
        }
      } yield {
        // TODO(olafur) implement withFilter on Configured
        val (finalRewrite, scalafixConfig) = configuration
        val finalConfig = scalafixConfig.withOut(common.err)
        finalRewrite -> finalConfig
      }
    }
    val resolvedRewrite: Configured[Rewrite] =
      resolvedRewriteAndConfig.map(_._1)
    val resolvedConfig: Configured[ScalafixConfig] =
      resolvedRewriteAndConfig.map(_._2)

    val resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] = try {
      val outFromPattern = Pattern.compile(outFrom.getOrElse(""))
      def replacePath(file: AbsolutePath): AbsolutePath =
        AbsolutePath(
          outFromPattern
            .matcher(file.toString())
            .replaceAll(outTo.getOrElse("")))
      Ok(replacePath _)
    } catch {
      case e: PatternSyntaxException =>
        ConfError.msg(s"Invalid regex '$outFrom'! ${e.getMessage}").notOk
    }

    val mirrorInputs: Configured[Map[AbsolutePath, Input.LabeledString]] =
      for {
        _ <- resolvedRewrite // force evaluation of rewrite.
        database <- cachedDatabase.getOrElse(Ok(Database(Nil)))
      } yield {
        val inputsByAbsolutePath = database.entries.collect {
          case (input @ Input.LabeledString(path, _), _) =>
            resolvedSourceroot.resolve(path) -> input
        }
        inputsByAbsolutePath.toMap
      }

    val fixFilesWithMirror: Configured[Seq[FixFile]] = for {
      fromMirror <- mirrorInputs
      files <- fixFiles
    } yield {
      files.map { file =>
        val labeled = fromMirror.get(file.original.path)
        file.copy(mirror = labeled)
      }
    }

  }
}
