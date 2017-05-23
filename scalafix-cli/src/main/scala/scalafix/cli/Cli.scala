package scalafix
package cli

import scalafix.config.MetaconfigPendingUpstream._
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.dialects
import scala.meta.inputs.Input
import scala.meta.io.AbsolutePath
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config._
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.reflect.ScalafixToolbox
import scalafix.rewrite.ProcedureSyntax
import scalafix.syntax._
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.internal.semantic.vfs
import scalafix.cli.CliCommand.PrintAndExit
import scalafix.cli.CliCommand.RunScalafix
import caseapp._
import caseapp.core.WithHelp
import com.martiansoftware.nailgun.NGContext
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err,
    @Hidden stackVerbosity: Int = 20
) {
  def workingPath = AbsolutePath(workingDirectory)
  def workingDirectoryFile = new File(workingDirectory)
}

@AppName("scalafix")
@AppVersion(scalafix.Versions.version)
@ProgName("scalafix")
case class ScalafixOptions(
    @HelpMessage("Print version number and exit")
    @ExtraName("v")
    version: Boolean = false,
    @HelpMessage("If set, print out debugging inforation to stderr.")
    verbose: Boolean = false,
    @HelpMessage(
      "Scalafix configuration, either a file path or a hocon string")
    @ValueDescription(".scalafix.conf OR imports.organize=false")
    @ExtraName("c")
    config: Option[String] = None,
    @HelpMessage(
      """Absolute path passed to scalahost with -P:scalahost:sourceroot:<path>.
        |        Required for semantic rewrites
      """.stripMargin)
    @ValueDescription("/foo/myproject")
    sourceroot: Option[String] = None,
    @HelpMessage(
      """java.io.File.pathSeparator separated list of jar files or directories
        |        containing classfiles and `semanticdb` files. The `semanticdb`
        |        files are emitted by the scalahost-nsc compiler plugin and
        |        are necessary for the semantic API to function. The
        |        classfiles + jar files are necessary forruntime compilation
        |        of quasiquotes when extracting symbols (that is,
        |        `q"scala.Predef".symbol`)""".stripMargin
    )
    @ValueDescription("entry1.jar:entry2.jar")
    classpath: Option[String] = None,
    @HelpMessage(
      """java.io.File.pathSeparator separated list of Scala source files OR
        |        directories containing Scala source files""".stripMargin)
    @ValueDescription("File2.scala:File1.scala:src/main/scala")
    sourcepath: Option[String] = None,
    @HelpMessage(
      s"""Rewrite rules to run.
         |        NOTE. rewrite.rules = [ .. ] from --config will also run""".stripMargin
    )
    @ValueDescription(
      s"""$ProcedureSyntax OR
         |               file:LocalFile.scala OR
         |               scala:full.Name OR
         |               https://gist.com/.../Rewrite.scala""".stripMargin
    )
    rewrites: List[String] = Nil,
    @HelpMessage(
      "Files to fix. Runs on all *.scala files if given a directory")
    @ValueDescription("File1.scala File2.scala")
    @ExtraName("f")
    files: List[String] = List.empty[String],
    @HelpMessage(
      "If true, writes changes to files instead of printing to stdout")
    @ExtraName("i")
    inPlace: Boolean = true,
    stdout: Boolean = false,
    @HelpMessage(
      """Regex that is passed as first argument to
        |        fileToFix.replaceAll(outFrom, outTo)""".stripMargin
    )
    @ValueDescription("/shared/")
    outFrom: String = "",
    @HelpMessage(
      """Replacement string that is passed as second argument to
        |        fileToFix.replaceAll(outFrom, outTo)""".stripMargin
    )
    @ValueDescription("/custom/")
    outTo: String = "",
    @HelpMessage(
      "If true, run on single thread. If false (default), use all available cores")
    singleThread: Boolean = false,
    @HelpMessage("If true, prints out debug information")
    debug: Boolean = false,
    // NOTE: This option could be avoided by adding another entrypoint like `Cli.safeMain`
    // or SafeCli.main. However, I opted for a cli flag since that plays nicely
    // with default `run.in(Compile).toTask("--no-sys-exit").value` in sbt.
    // Another other option would be to do
    // `runMain.in(Compile).toTask("scalafix.cli.SafeMain")` but I prefer to
    // keep only one main function if possible since that plays nicely with
    // automatic detection of `main` functions in tools like `coursier launch`.
    @HelpMessage(
      "If true, does not sys.exit at the end. Useful for example in sbt-scalafix")
    noSysExit: Boolean = false,
    @Recurse common: CommonOptions = CommonOptions()
)

sealed abstract class WriteMode {
  def isWriteFile: Boolean = this == WriteMode.WriteFile
}
object WriteMode {
  case object WriteFile extends WriteMode
  case object Stdout extends WriteMode
}

sealed abstract class CliCommand {
  import CliCommand._
  def isOk: Boolean = !isError
  def isError: Boolean = this match {
    case RunScalafix(_) => false
    case _ => true
  }
}
object CliCommand {
  case class PrintAndExit(msg: String, status: ExitStatus) extends CliCommand
  case class RunScalafix(runner: CliRunner) extends CliCommand
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

object Cli {
  val emptyDatabase = Database(Nil)
  import ArgParserImplicits._
  private val withHelp = OptionsMessages.withHelp
  val helpMessage: String = withHelp.helpMessage +
    s"""|
        |Examples:
        |  $$ scalafix --rewrites ProcedureSyntax Code.scala # write fixed file in-place
        |  $$ scalafix --stdout --rewrites ProcedureSyntax Code.scala # print fixed file to stdout
        |  $$ cat .scalafix.conf
        |  rewrites = [ProcedureSyntax]
        |  $$ scalafix Code.scala # Same as --rewrites ProcedureSyntax
        |
        |Exit status codes:
        | ${ExitStatus.all.mkString("\n ")}
        |""".stripMargin
  val usageMessage: String = withHelp.usageMessage
  val default = ScalafixOptions()
  // Run this at the end of the world, calls sys.exit.

  case class NonZeroExitCode(exitStatus: ExitStatus)
      extends Exception(s"Expected exit code 0. Got exit code $exitStatus")
  def runOn(options: ScalafixOptions): ExitStatus =
    runMain(parseOptions(options), options.common)
  def parseOptions(options: ScalafixOptions): CliCommand =
    CliRunner.fromOptions(options) match {
      case Ok(runner) => RunScalafix(runner)
      case NotOk(err) =>
        PrintAndExit(err.toString(), ExitStatus.InvalidCommandLineOption)
    }
  def main(args: Array[String]): Unit = {
    val exit = runMain(args.to[Seq], CommonOptions())
    if (args.contains("--no-sys-exit")) {
      if (exit.code != 0) throw NonZeroExitCode(exit)
      else ()
    } else sys.exit(exit.code)
  }

  def isScalaPath(path: AbsolutePath): Boolean =
    path.toString().endsWith(".scala") || path.toString().endsWith(".sbt")

  def parse(args: Seq[String]): CliCommand = {
    import CliCommand._
    OptionsParser.withHelp.detailedParse(args) match {
      case Left(err) =>
        PrintAndExit(err, ExitStatus.InvalidCommandLineOption)
      case Right((WithHelp(_, help @ true, _), _, _)) =>
        PrintAndExit(helpMessage, ExitStatus.Ok)
      case Right((WithHelp(usage @ true, _, _), _, _)) =>
        PrintAndExit(usageMessage, ExitStatus.Ok)
      case Right((WithHelp(_, _, options), _, _)) if options.version =>
        PrintAndExit(s"${withHelp.appName} ${withHelp.appVersion}",
                     ExitStatus.Ok)
      case Right((WithHelp(_, _, options), extraFiles, _)) =>
        parseOptions(options.copy(files = options.files ++ extraFiles))
    }
  }

  def runMain(args: Seq[String], commonOptions: CommonOptions): ExitStatus =
    runMain(parse(args), commonOptions)

  def runMain(cliCommand: CliCommand,
              commonOptions: CommonOptions): ExitStatus =
    cliCommand match {
      case CliCommand.PrintAndExit(msg, exit) =>
        commonOptions.out.write(msg.getBytes)
        exit
      case CliCommand.RunScalafix(runner) =>
        val exit = runner.run()
        exit
    }

  def nailMain(nGContext: NGContext): Unit = {
    val exit =
      runMain(
        nGContext.getArgs.to[Seq],
        CommonOptions(
          workingDirectory = nGContext.getWorkingDirectory,
          out = nGContext.out,
          in = nGContext.in,
          err = nGContext.err
        )
      )
    nGContext.exit(exit.code)
  }
}
