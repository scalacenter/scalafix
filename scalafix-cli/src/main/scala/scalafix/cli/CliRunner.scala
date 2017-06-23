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
import java.util.stream.Collectors
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.internal.inputs._
import scala.meta.internal.io.PlatformFileIO
import scala.meta.internal.semantic.vfs
import scala.meta.io.AbsolutePath
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config.Class2Hocon
import scalafix.config.FilterMatcher
import scalafix.config.PrintStreamReporter
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.reflect.ScalafixToolbox
import scalafix.rewrite.ScalafixRewrites
import scalafix.syntax._
import difflib.DiffUtils
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import metaconfig._
import org.scalameta.logger

sealed abstract case class CliRunner(
    sourceroot: AbsolutePath,
    cli: ScalafixOptions,
    config: ScalafixConfig,
    database: Option[Database],
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
    if (database.isEmpty) true
    else if (!input.toIO.exists() && cli.outTo.nonEmpty) true
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
      database <- builder.resolvedMirror
      sourceroot <- builder.resolvedSourceroot
      rewrite <- builder.resolvedRewrite
      replace <- builder.resolvedPathReplace
      inputs <- builder.fixFilesWithMirror
      config <- builder.resolvedConfig
    } yield {
      if (options.verbose) {
        val entries = database.map(_.entries.length).getOrElse(0)
        val inputsNames = inputs
          .map(
            _.original.path.syntax
              .stripPrefix(options.common.workingDirectory + "/"))
          .mkString(", ")
        options.common.err.println(
          s"""|Files to fix (${inputs.length}):
              |$inputsNames
              |Database entires count: $entries
              |Database head:
              |${database.toString.take(1000)}
              |Config:
              |${Class2Hocon(config)}
              |Rewrite:
              |$rewrite
              |""".stripMargin
        )
      }
      new CliRunner(
        sourceroot = sourceroot,
        cli = options,
        config = config,
        database = database,
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

    val resolvedClasspath: Option[Configured[Classpath]] =
      if (autoMirror) {
        val cp = autoClasspath(common.workingPath)
        if (verbose) {
          common.err.println(s"Automatic classpath=$cp")
        }
        if (cp.shallow.nonEmpty) Some(Ok(cp))
        else {
          val msg =
            """Unable to automatically detect .semanticdb files to run semantic rewrites. Possible workarounds:
              |- re-compile sources with the scalahost compiler plugin enabled.
              |- pass in the --syntactic flag to run only syntactic rewrites.
              |- explicitly pass in --classpath and --sourceroot to run semantic rewrites.""".stripMargin
          Some(ConfError.msg(msg).notOk)
        }
      } else {
        classpath.map { x =>
          val paths = x.split(File.pathSeparator).map { path =>
            AbsolutePath.fromString(path)(common.workingPath)
          }
          Ok(Classpath(paths))
        }
      }

    val autoSourceroot: Option[AbsolutePath] =
      if (autoMirror) resolvedClasspath.map(_ => common.workingPath)
      else {
        sourceroot
          .map(AbsolutePath.fromString(_))
          .orElse(Some(common.workingPath))
      }

    // Database
    private val resolvedDatabase: Configured[Database] =
      (resolvedClasspath, autoSourceroot) match {
        case (Some(Ok(cp)), sp) =>
          val tryMirror = for {
            mirror <- Try {
              val sourcepath = sp.map(Sourcepath.apply)
              val mirror =
                vfs.Database.load(cp).toSchema.toMeta(sourcepath)
              mirror
            }
          } yield mirror
          tryMirror match {
            case Success(x) => Ok(x)
            case scala.util.Failure(e) => ConfError.msg(e.toString).notOk
          }
        case (Some(err @ NotOk(_)), _) => err
        case (None, _) =>
          Ok(ScalafixRewrites.emptyDatabase)
      }
    val resolvedMirror: Configured[Option[Database]] =
      resolvedDatabase.map { x =>
        if (x == ScalafixRewrites.emptyDatabase) None
        else Some(x)
      }
    val resolvedMirrorSourceroot: Configured[AbsolutePath] =
      autoSourceroot match {
        case None => ConfError.msg("--sourceroot is required").notOk
        case Some(path) => Ok(path)
      }
    val resolvedSourceroot: Configured[AbsolutePath] =
      resolvedMirror.flatMap {
        // Use mirror sourceroot if Mirror.nonEmpty
        case Some(_) => resolvedMirrorSourceroot
        // Use working directory if running syntactic rewrites.
        case None => Ok(common.workingPath)
      }

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

    val mirrorInputs: Configured[Map[AbsolutePath, Input.LabeledString]] =
      for {
        sourceroot <- resolvedSourceroot
        database <- resolvedDatabase
      } yield {
        val inputsByAbsolutePath = database.entries.collect {
          case (input @ Input.LabeledString(path, _), _) =>
            sourceroot.resolve(path) -> input
        }
        inputsByAbsolutePath.toMap
      }

    val fixFilesWithMirror: Configured[Seq[FixFile]] = for {
      fromMirror <- mirrorInputs
      files <- fixFiles
    } yield {
      files.map { file =>
        file.copy(mirror = fromMirror.get(file.original.path))
      }
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
    val resolvedRewriteAndConfig: Configured[(Rewrite, ScalafixConfig)] =
      for {
        mirror <- resolvedMirror
        decoder = ScalafixCompilerDecoder.fromMirrorOption(mirror)
        cliArgRewrite <- rewrites
          .foldLeft(ScalafixToolbox.emptyRewrite) {
            case (rewrite, next) =>
              rewrite
                .product(decoder.read(Conf.Str(next)))
                .map { case (a, b) => a.andThen(b) }
          }
        inputs <- fixFilesWithMirror
        configuration <- {
          if (inputs.isEmpty) {
            Ok(Rewrite.empty -> ScalafixConfig.default)
          } else {
            resolvedConfigInput.flatMap(input =>
              ScalafixConfig.fromInput(input, mirror)(decoder))
          }
        }
        // TODO(olafur) implement withFilter on Configured
        (configRewrite, scalafixConfig) = configuration
        finalConfig = scalafixConfig.copy(
          reporter = scalafixConfig.reporter match {
            case r: PrintStreamReporter => r.copy(outStream = common.err)
            case _ => ScalafixReporter.default.copy(outStream = common.err)
          }
        )
        finalRewrite = cliArgRewrite.andThen(configRewrite)
      } yield finalRewrite -> finalConfig
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
  }
}
