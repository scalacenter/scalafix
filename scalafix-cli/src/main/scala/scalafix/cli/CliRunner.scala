package scalafix
package cli

import java.io.OutputStreamWriter
import java.net.URI
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.internal.inputs._
import scala.meta.internal.io.FileIO
import scala.meta.internal.semantic.vfs
import scala.meta.parsers.ParseException
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config.Class2Hocon
import scalafix.config.FilterMatcher
import scalafix.config.MetaconfigPendingUpstream._
import scalafix.config.PrintStreamReporter
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.reflect.ScalafixToolbox
import scalafix.rewrite.ScalafixRewrites
import scalafix.syntax._
import metaconfig.Configured.Ok
import metaconfig._

sealed abstract case class CliRunner(
    sourceroot: AbsolutePath,
    cli: ScalafixOptions,
    config: ScalafixConfig,
    database: Option[Database],
    rewrite: Rewrite,
    explicitPaths: Seq[Input],
    inputs: Seq[Input],
    replacePath: AbsolutePath => AbsolutePath
) {
  val sbtConfig: ScalafixConfig = config.copy(dialect = dialects.Sbt0137)
  val writeMode: WriteMode =
    if (cli.stdout) WriteMode.Stdout
    else WriteMode.WriteFile
  val common: CommonOptions = cli.common
  val explicitURIs: Set[URI] =
    explicitPaths.toIterator
      .map(x => new URI(x.path(common.workingPath).toString()).normalize())
      .toSet
  def wasExplicitlyPassed(path: AbsolutePath): Boolean =
    explicitURIs.contains(path.toURI.normalize())

  def run(): ExitStatus = {
    val display = new TermDisplay(new OutputStreamWriter(System.out))
    if (inputs.length > 10) display.init()
    if (inputs.isEmpty) common.err.println("Running scalafix on 0 files.")
    val msg = "Running scalafix..."
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
  def safeHandleInput(input: Input): ExitStatus = {
    def path = input.path(sourceroot)
    try {
      val inputConfig = if (input.isSbtFile) sbtConfig else config
      inputConfig.dialect(input).parse[Source] match {
        case parsers.Parsed.Error(pos, message, _) =>
          if (wasExplicitlyPassed(path)) {
            common.err.println(pos.formatMessage("error", message))
          }
          ExitStatus.ParseError
        case parsers.Parsed.Success(tree) =>
          val ctx = RewriteCtx(tree, config)
          val fixed = rewrite(ctx)
          if (writeMode.isWriteFile) {
            val outFile = replacePath(path)
            Files.write(outFile.toNIO, fixed.getBytes(input.charset))
          } else common.out.write(fixed.getBytes)
          ExitStatus.Ok
      }
    } catch {
      case NonFatal(e) =>
        reportError(path, e, cli)
        e match {
          case _: scalafix.Failure => ExitStatus.ScalafixError
          case _ => ExitStatus.UnexpectedError
        }
    }
  }

  def reportError(path: AbsolutePath,
                  cause: Throwable,
                  options: ScalafixOptions): Unit = {
    options.common.err.println(
      s"""Error fixing file: ${path.toString()}
         |Cause: $cause""".stripMargin
    )
    cause.setStackTrace(
      cause.getStackTrace.take(options.common.stackVerbosity))
    cause.printStackTrace(options.common.err)
  }
}

object CliRunner {
  def fromOptions(options: ScalafixOptions): Configured[CliRunner] = {
    val builder = new CliRunner.Builder(options)
    for {
      database <- builder.resolvedMirror
      sourceroot <- builder.resolvedSourceroot
      rewrite <- builder.resolvedRewrite
      replace <- builder.resolvedPathReplace
      inputs <- builder.resolvedInputs
      config <- builder.resolvedConfig
    } yield {
      if (options.verbose) {
        options.common.err.println(
          s"""|Files to fix:
              |$inputs
              |Database:
              |$database
              |Config:
              |${Class2Hocon(config)}
              |Rewrite:
              |$config
              |""".stripMargin
        )
        options.common.err.println(database.toString)
      }
      new CliRunner(
        sourceroot = sourceroot,
        cli = options,
        config = config,
        database = database,
        rewrite = rewrite,
        replacePath = replace,
        inputs = inputs,
        explicitPaths = builder.explicitPaths
      ) {}
    }
  }

  private class Builder(val cli: ScalafixOptions) {
    import cli._

    implicit val workingDirectory: AbsolutePath = common.workingPath

    // Database
    private val resolvedDatabase: Configured[Database] =
      (classpath, sourceroot) match {
        case (Some(cp), sp) =>
          val tryMirror = for {
            mirror <- Try {
              val sourcepath = sp.map(Sourcepath.apply)
              val classpath = Classpath(cp)
              val mirror =
                vfs.Database.load(classpath).toSchema.toMeta(sourcepath)
              mirror
            }
          } yield mirror
          tryMirror match {
            case Success(x) => Ok(x)
            case scala.util.Failure(e) => ConfError.msg(e.toString).notOk
          }
        case (None, Some(sp)) =>
          ConfError
            .msg(
              s"Missing --classpath, cannot use --sourcepath $sp without --classpath")
            .notOk
        case (None, None) =>
          Ok(ScalafixRewrites.emptyDatabase)
      }
    val resolvedMirror: Configured[Option[Database]] =
      resolvedDatabase.map { x =>
        if (x == ScalafixRewrites.emptyDatabase) None
        else Some(x)
      }
    val resolvedMirrorSourceroot: Configured[AbsolutePath] =
      sourceroot match {
        case None => ConfError.msg("--sourceroot is required").notOk
        case Some(path) => Ok(AbsolutePath.fromString(path))
      }
    val resolvedSourceroot: Configured[AbsolutePath] =
      resolvedMirror.flatMap {
        // Use mirror sourceroot if Mirror.nonEmpty
        case Some(_) => resolvedMirrorSourceroot
        // Use working directory if running syntactic rewrites.
        case None => Ok(common.workingPath)
      }

    // Inputs
    val explicitPaths: Seq[Input] = cli.files.toVector.flatMap { file =>
      val path = AbsolutePath.fromString(file)
      if (path.isDirectory)
        FileIO.listAllFilesRecursively(path).map(Input.File.apply)
      else List(Input.File(path))
    }

    def isInputFile(input: Input): Boolean = input match {
      case _: Input.File | _: Input.LabeledString => true
      case _ => false
    }

    val mirrorPaths: Configured[Seq[Input]] =
      resolvedDatabase.flatMap { database =>
        val nonInputFiles = database.entries.filter(x => !isInputFile(x._1))
        if (nonInputFiles.isEmpty) Ok(database.entries.map(_._1))
        else {
          ConfError
            .msg(
              s"""Expected Input.File from parsed database, received:
                 |${nonInputFiles.mkString("\n")}""".stripMargin
            )
            .notOk
        }
      }
    private val resolvedPathMatcher: Configured[FilterMatcher] = try {
      Ok(FilterMatcher(include, exclude))
    } catch {
      case e: PatternSyntaxException =>
        ConfError
          .msg(
            s"Invalid '${e.getPattern}' for  --include/--exclude. ${e.getMessage}")
          .notOk
    }
    val resolvedInputs: Configured[Seq[Input]] = for {
      fromMirror <- mirrorPaths
      pathMatcher <- resolvedPathMatcher
      sourceroot <- resolvedSourceroot
    } yield {
      def inputOK(input: Input) = pathMatcher.matches(input.label)
      val builder = Seq.newBuilder[Input]
      fromMirror.withFilter(inputOK).foreach(builder += _)
      explicitPaths.withFilter(inputOK).foreach(builder += _)
      builder.result()
    }

    lazy val resolvedConfigInput: Configured[Input] =
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
        inputs <- resolvedInputs
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
      val outFromPattern = Pattern.compile(outFrom)
      def replacePath(file: AbsolutePath): AbsolutePath =
        AbsolutePath(outFromPattern.matcher(file.toString()).replaceAll(outTo))
      Ok(replacePath _)
    } catch {
      case e: PatternSyntaxException =>
        ConfError.msg(s"Invalid regex '$outFrom'! ${e.getMessage}").notOk
    }
  }
}
