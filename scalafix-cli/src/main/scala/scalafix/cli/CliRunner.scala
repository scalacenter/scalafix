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
import scala.meta.io.AbsolutePath
import scala.meta.sbthost.Sbthost
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.internal.cli.CommonOptions
import scalafix.internal.cli.FixFile
import scalafix.internal.cli.ScalafixOptions
import scalafix.internal.cli.TermDisplay
import scalafix.internal.cli.WriteMode
import scalafix.internal.config.Class2Hocon
import scalafix.internal.config.FilterMatcher
import scalafix.internal.config.LazySemanticCtx
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.config.RewriteKind
import scalafix.internal.config.ScalafixConfig
import scalafix.reflect.ScalafixReflect
import scalafix.syntax._
import metaconfig.Configured.Ok
import metaconfig._
import org.scalameta.logger

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
    else if (cli.test) WriteMode.Test
    else WriteMode.WriteFile
  val common: CommonOptions = cli.common
  implicit val workingDirectory: AbsolutePath = common.workingPath

  def run(): ExitStatus = {
    val display = new TermDisplay(
      new OutputStreamWriter(
        if (cli.stdout) cli.common.err else cli.common.out),
      fallbackMode = cli.nonInteractive || TermDisplay.defaultFallbackMode)
    if (inputs.length > 10) display.init()
    val msg = cli.projectIdPrefix + s"Running ${rewrite.name}"
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
    val exit = exitCode.get()
    if (config.lint.reporter.hasErrors) {
      ExitStatus.merge(ExitStatus.LinterError, exit)
    } else exit
  }

  // safeguard to verify that the original file contents have not changed since the
  // creation of the .semanticdb file. Without this check, we risk overwriting
  // the latest changes written by the user.
  private def isUpToDate(input: FixFile): Boolean =
    if (!input.toIO.exists() && cli.outTo.nonEmpty) true
    else {
      input.semanticCtx match {
        case Some(Input.VirtualFile(_, contents)) =>
          val fileToWrite = scala.io.Source.fromFile(input.toIO)
          try fileToWrite.sameElements(contents.toCharArray.toIterator)
          finally fileToWrite.close()
        case _ =>
          true // No way to check for Input.File, see https://github.com/scalameta/scalameta/issues/886
      }
    }

  def unsafeHandleInput(input: FixFile): ExitStatus = {
    val inputConfig =
      if (input.original.label.endsWith(".sbt")) sbtConfig else config
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
        val fixed = rewrite.apply(ctx)
        writeMode match {
          case WriteMode.Stdout =>
            common.out.write(fixed.getBytes)
            ExitStatus.Ok
          case WriteMode.Test =>
            val isUnchanged =
              input.original.path.readAllBytes
                .sameElements(fixed.getBytes(input.original.charset))
            if (isUnchanged) ExitStatus.Ok
            else {
              val diff =
                Patch.unifiedDiff(
                  input.original,
                  Input.VirtualFile(
                    s"<expected fix from ${rewrite.name}>",
                    fixed
                  )
                )
              common.out.println(diff)
              ExitStatus.TestFailed
            }
          case WriteMode.WriteFile =>
            val outFile = replacePath(input.original.path)
            if (isUpToDate(input)) {
              Files.createDirectories(outFile.toNIO.getParent)
              Files.write(outFile.toNIO, fixed.getBytes(input.original.charset))
              ExitStatus.Ok
            } else {
              ctx.reporter.error(
                s"Stale semanticdb for ${CliRunner.pretty(outFile)}, skipping rewrite. Please recompile.")
              if (cli.verbose) {
                val diff =
                  Patch.unifiedDiff(input.semanticCtx.get, input.original)
                common.err.println(diff)
              }
              ExitStatus.StaleSemanticDB
            }
        }
    }
  }

  def safeHandleInput(input: FixFile): ExitStatus =
    try unsafeHandleInput(input)
    catch {
      case NonFatal(e) =>
        reportError(input.original.path, e, cli)
        e match {
          case _: scalafix.Failure => ExitStatus.ScalafixError
          case _ => ExitStatus.UnexpectedError
        }
    }

  def reportError(
      path: AbsolutePath,
      cause: Throwable,
      options: ScalafixOptions): Unit = {
    config.reporter.error(s"Failed to fix $path")
    cause.setStackTrace(cause.getStackTrace.take(options.common.stackVerbosity))
    cause.printStackTrace(options.common.err)
  }
}

object CliRunner {
  private[scalafix] def pretty(path: AbsolutePath)(
      implicit cwd: AbsolutePath): String = {
    // toRelative should not throw exceptions, but it does, see https://github.com/scalameta/scalameta/issues/821
    Try(path.toRelative(cwd)).getOrElse(path).toString
  }

  /** Construct CliRunner with rewrite from ScalafixOptions. */
  def fromOptions(options: ScalafixOptions): Configured[CliRunner] =
    safeFromOptions(options, None)

  /** Construct CliRunner with custom rewrite. */
  def fromOptions(
      options: ScalafixOptions,
      rewrite: Rewrite): Configured[CliRunner] =
    safeFromOptions(options, Some(rewrite))

  private def safeFromOptions(
      options: ScalafixOptions,
      rewrite: Option[Rewrite]
  ): Configured[CliRunner] = {
    try {
      val builder = new CliRunner.Builder(options)
      rewrite.fold(
        builder.resolvedRewrite.andThen(rewrite =>
          unsafeFromOptions(options, rewrite, builder))
      )(r => unsafeFromOptions(options, r, builder))
    } catch {
      case NonFatal(e) =>
        ConfError.exception(e, options.common.stackVerbosity).notOk
    }
  }
  private def unsafeFromOptions(
      options: ScalafixOptions,
      rewrite: Rewrite,
      builder: Builder
  ): Configured[CliRunner] = {
    (
      builder.resolvedSourceroot |@|
        builder.resolvedPathReplace |@|
        builder.fixFilesWithSemanticCtx |@|
        builder.resolvedConfig
    ).map {
      case (((sourceroot, replace), inputs), config) =>
        if (options.verbose) {
          options.diagnostic.info(
            s"""|Config:
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
            AbsolutePath(path)(common.workingPath)
          }
          Ok(Classpath(paths.toList))
        case None =>
          val cp = autoClasspath(common.workingPath)
          if (verbose) {
            common.err.println(s"Automatic classpath=$cp")
          }
          if (cp.shallow.nonEmpty) Ok(cp)
          else {
            ConfError
              .msg(
                "Unable to infer --classpath containing .semanticdb files. " +
                  "Please provide --classpath explicitly.")
              .notOk
          }
      }

    val resolvedSourceroot: Configured[AbsolutePath] = {
      val result = sourceroot
        .map(AbsolutePath(_))
        .getOrElse(common.workingPath)
      if (result.isDirectory) Ok(result)
      else {
        ConfError.msg(s"Invalid --sourceroot $result is not a directory!").notOk
      }
    }

    // We don't know yet if we need to compute the database or not.
    // If all the rewrites are syntactic, we never need to compute the semanticCtx.
    // If a single rewrite is semantic, then we need to compute the database.
    private var cachedDatabase = Option.empty[Configured[SemanticCtx]]
    private def computeAndCacheDatabase(): Option[SemanticCtx] = {
      val result: Configured[SemanticCtx] = cachedDatabase.getOrElse {
        (resolveClasspath |@| resolvedSourceroot).andThen {
          case (classpath, root) =>
            val db = SemanticCtx.load(Sbthost
              .patchDatabase(Database.load(classpath, Sourcepath(root)), root))
            if (verbose) {
              common.err.println(
                s"Loaded database with ${db.entries.length} entries.")
            }
            if (db.entries.nonEmpty) Ok(db)
            else {
              ConfError
                .msg("Missing SemanticCtx, found no semanticdb files!")
                .notOk
            }
        }
      }
      if (cachedDatabase.isEmpty) {
        cachedDatabase = Some(result)
      }
      Some(MetaconfigPendingUpstream.get_!(result))
    }
    private def resolveDatabase(kind: RewriteKind): Option[SemanticCtx] = {
      if (kind.isSyntactic) None
      else computeAndCacheDatabase()
    }
    private val lazySemanticCtx: LazySemanticCtx =
      new LazySemanticCtx(resolveDatabase, diagnostic)

    // expands a single file into a list of files.
    def expand(matcher: FilterMatcher)(path: AbsolutePath): Seq[FixFile] = {
      if (!path.toFile.exists()) {
        common.cliArg.error(s"$path does not exist. ${common.workingDirectory}")
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

    val fixFiles: Configured[Seq[FixFile]] =
      resolvedPathMatcher.andThen { pathMatcher =>
        val paths =
          if (cli.files.nonEmpty) cli.files.map(AbsolutePath(_))
          // If no files are provided, assume cwd.
          else common.workingPath :: Nil
        val result = paths.toVector.flatMap(expand(pathMatcher))
        if (result.isEmpty) {
          ConfError
            .msg(
              s"No files to fix! Missing at least one .scala or .sbt file from: " +
                paths.mkString(", "))
            .notOk
        } else Ok(result)
      }

    val resolvedConfigInput: Configured[Input] =
      (config, configStr) match {
        case (Some(x), Some(y)) =>
          ConfError
            .msg(s"Can't configure both --config $x and --config-str $y")
            .notOk
        case (Some(configPath), _) =>
          val path = AbsolutePath(configPath)
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
      val decoder = ScalafixReflect.fromLazySemanticCtx(lazySemanticCtx)
      fixFiles.andThen { inputs =>
        val configured = resolvedConfigInput.andThen(input =>
          ScalafixConfig.fromInput(input, lazySemanticCtx, rewrites)(decoder))
        configured.map { configuration =>
          // TODO(olafur) implement withFilter on Configured
          val (finalRewrite, scalafixConfig) = configuration
          val finalConfig = scalafixConfig.withOut(common.err)
          finalRewrite -> finalConfig
        }
      }
    }
    val resolvedRewrite: Configured[Rewrite] =
      resolvedRewriteAndConfig.andThen {
        case (rewrite, _) =>
          if (rewrite.rewriteName.isEmpty)
            ConfError
              .msg(
                "No rewrite was provided! Use --rewrite to specify a rewrite.")
              .notOk
          else Ok(rewrite)
      }
    val resolvedConfig: Configured[ScalafixConfig] =
      resolvedRewriteAndConfig.map {
        case (_, config) => config.withFreshReporters
      }

    val resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] = try {
      (outFrom, outTo) match {
        case (None, None) => Ok(identity[AbsolutePath])
        case (Some(from), Some(to)) =>
          val outFromPattern = Pattern.compile(from)
          def replacePath(file: AbsolutePath): AbsolutePath =
            AbsolutePath(outFromPattern.matcher(file.toString()).replaceAll(to))
          Ok(replacePath _)
        case (Some(from), _) =>
          ConfError
            .msg(s"--out-from $from must be accompanied with --out-to")
            .notOk
        case (_, Some(to)) =>
          ConfError
            .msg(s"--out-to $to must be accompanied with --out-from")
            .notOk
      }
    } catch {
      case e: PatternSyntaxException =>
        ConfError.msg(s"Invalid regex '$outFrom'! ${e.getMessage}").notOk
    }

    val semanticInputs: Configured[Map[AbsolutePath, Input.VirtualFile]] = {
      (resolvedRewrite |@| resolvedSourceroot).andThen {
        case (_, root) =>
          cachedDatabase.getOrElse(Ok(SemanticCtx(Nil))).map { database =>
            def checkExists(path: AbsolutePath): Unit =
              if (!path.isFile) {
                common.cliArg.error(
                  s"semanticdb input $path is not a file. Is --sourceroot correct?")
              }
            val inputsByAbsolutePath =
              database.entries.toIterator.map(_.input).collect {
                case input @ Input.VirtualFile(path, _) =>
                  val key = root.resolve(path)
                  checkExists(key)
                  key -> input
                case input @ Input.File(path, _) =>
                  checkExists(path)
                  val contents = new String(input.chars)
                  path -> Input.VirtualFile(path.toString(), contents)
              }
            inputsByAbsolutePath.toMap
          }
      }
    }

    val fixFilesWithSemanticCtx: Configured[Seq[FixFile]] =
      (semanticInputs |@| fixFiles).map {
        case (fromSemanticCtx, files) =>
          files.map { file =>
            val labeled = fromSemanticCtx.get(file.original.path)
            if (fromSemanticCtx.nonEmpty && labeled.isEmpty) {
              common.cliArg.error(
                s"No semanticdb associated with ${file.original.path}. " +
                  s"Is --sourceroot correct?")
            }
            file.copy(semanticCtx = labeled)
          }
      }

  }
}
