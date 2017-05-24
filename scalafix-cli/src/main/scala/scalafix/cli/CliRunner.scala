package scalafix
package cli

import java.io.File
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
import scala.meta.internal.io.FileIO
import scala.meta.internal.semantic.vfs
import scala.meta.parsers.ParseException
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config.MetaconfigPendingUpstream._
import scalafix.config.PrintStreamReporter
import scalafix.config.ScalafixConfig
import scalafix.config.ScalafixReporter
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.reflect.ScalafixToolbox
import scalafix.syntax._
import metaconfig.Configured.Ok
import metaconfig._

case class CliRunner(
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
    val path = input.path(common.workingPath)
    try {
      val inputConfig = if (input.isSbtFile) sbtConfig else config
      val tree = inputConfig.dialect(input).parse[Source].get
      val ctx = RewriteCtx(tree, config)
      val fixed = rewrite(ctx)
      if (writeMode.isWriteFile) {
        val outFile = replacePath(path)
        Files.write(outFile.toNIO, fixed.getBytes(input.charset))
      } else common.out.write(fixed.getBytes)
      ExitStatus.Ok
    } catch {
      case e @ ParseException(_, _) =>
        if (wasExplicitlyPassed(path)) {
          // Only log if user explicitly specified that file.
          reportError(path, e, cli)
        }
        ExitStatus.ParseError
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
      config <- builder.resolvedConfig
      rewrite <- builder.resolvedRewrite
      replace <- builder.resolvedPathReplace
      inputs <- builder.resolvedInputs
    } yield {
      if (options.verbose) {
        options.common.err.println(
          s"""|Database:
              |$database
              |Config:
              |$config
              |Rewrite:
              |$config
              |""".stripMargin
        )
        options.common.err.println(database.toString)
      }
      new CliRunner(
        cli = options,
        config = config,
        database = database,
        rewrite = rewrite,
        replacePath = replace,
        inputs = inputs,
        explicitPaths = builder.explicitPaths
      )
    }
  }

  private class Builder(val cli: ScalafixOptions) {
    import cli._

    val resolvedDatabase: Configured[Database] =
      (classpath, sourcepath) match {
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
          Ok(Cli.emptyDatabase)
      }
    val resolvedMirror: Configured[Option[Database]] =
      resolvedDatabase.map { x =>
        if (x == Cli.emptyDatabase) None else Some(x)
      }

    /** Returns ScalafixConfig from .scalafix.conf, it exists and --config was not passed. */
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
        configuration <- {
          val input: Option[Input] =
            ScalafixConfig
              .auto(common.workingDirectoryFile)
              .orElse(config.map { x =>
                if (new File(x).isFile) Input.File(new File(x))
                else Input.String(x)
              })
          input
            .map(x => ScalafixConfig.fromInput(x, mirror)(decoder))
            .getOrElse(
              Configured.Ok(
                Rewrite.emptyFromMirrorOpt(mirror) ->
                  ScalafixConfig.default
              ))
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
    val resolvedRewrite = resolvedRewriteAndConfig.map(_._1)
    val resolvedConfig = resolvedRewriteAndConfig.map(_._2)

    implicit val workingDirectory: AbsolutePath = common.workingPath
    val explicitPaths: Seq[Input] = cli.files.toVector.flatMap { file =>
      val path = AbsolutePath.fromString(file)
      if (path.isDirectory)
        FileIO.listAllFilesRecursively(path).map(Input.File.apply)
      else List(Input.File(path))
    }
    val resolvedSourceroot = sourceroot match {
      case None => ConfError.msg("--sourceroot is required").notOk
      case Some(path) => Ok(AbsolutePath.fromString(path))
    }
    val mirrorPaths: Configured[Seq[Input]] = resolvedDatabase
      .flatMap(database => {
        val x = database.entries.map {
          case (x: Input, _) => Ok(x)
          case (x: Input.LabeledString, _) =>
            // TODO(olafur) validate that the file contents have not changed.
            resolvedSourceroot.map(root => Input.File(root.resolve(x.label)))
          case (els, _) =>
            ConfError.msg(s"Unexpected Input type ${els.structure}").notOk
        }
        x.flipSeq
      })
    val resolvedInputs: Configured[Seq[Input]] = for {
      fromMirror <- mirrorPaths
    } yield fromMirror ++ explicitPaths

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
