package scalafix.cli

import scala.collection.GenSeq
import scala.meta.Mirror
import scala.meta.Tree
import scala.meta.dialects
import scala.meta.internal.scalahost.v1.offline
import scala.meta.inputs.Input
import scala.meta.parsers.Parsed
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scalafix.Failure
import scalafix.syntax._
import scalafix.Fixed
import scalafix.Fixed.Failed
import scalafix.Scalafix
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config._
import scalafix.reflect.ScalafixCompilerDecoder
import scalafix.reflect.ScalafixToolbox
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.ScalafixMirror
import scalafix.util.FileOps

import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

import caseapp._
import caseapp.core.WithHelp
import com.martiansoftware.nailgun.NGContext
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.Ok
import org.scalameta.logger

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err,
    @Hidden stackVerbosity: Int = 20
) {
  def workingDirectoryFile = new File(workingDirectory)
}

@AppName("scalafix")
@AppVersion(scalafix.Versions.version)
@ProgName("scalafix")
case class ScalafixOptions(
    @HelpMessage(
      "Scalafix configuration, either a file path or a hocon string"
    ) @ValueDescription(
      ".scalafix.conf OR imports.organize=false"
    ) @ExtraName("c") config: Option[String] = None,
    @HelpMessage(
      """java.io.File.pathSeparator separated list of jar files or directories
        |        containing classfiles and `semanticdb` files. The `semanticdb`
        |        files are emitted by the scalahost-nsc compiler plugin and
        |        are necessary for the semantic API to function. The
        |        classfiles + jar files are necessary forruntime compilation
        |        of quasiquotes when extracting symbols (that is,
        |        `q"scala.Predef".symbol`).""".stripMargin
    ) @ValueDescription(
      "entry1.jar:entry2.jar"
    ) classpath: Option[String] = None,
    @HelpMessage(
      """java.io.File.pathSeparator separated list of Scala source files OR
        |        directories containing Scala source files.""".stripMargin
    ) @ValueDescription(
      "File2.scala:File1.scala:src/main/scala"
    ) sourcepath: Option[String] = None,
    @HelpMessage(
      """File path to the scalahost-nsc compiler plugin fatjar, the same path
        |        that is passed in `-Xplugin:/scalahost.jar`.
        |        (optional) skip this option by using the "scalafix-fatcli"
        |        module instead of "scalafix-cli."""".stripMargin
    ) @ValueDescription(
      "$HOME/.ivy2/cache/.../scalahost-nsc_2.11.8.jar"
    ) scalahostNscPluginPath: Option[String] = None,
    @HelpMessage(
      s"""Rewrite rules to run.
         |        NOTE. rewrite.rules = [ .. ] from --config will also run.""".stripMargin
    ) @ValueDescription(
      s"""$ProcedureSyntax OR
         |               file:LocalFile.scala OR
         |               scala:full.Name OR
         |               https://gist.com/.../Rewrite.scala""".stripMargin
    ) rewrites: List[String] = Nil,
    @HelpMessage(
      "Files to fix. Runs on all *.scala files if given a directory."
    ) @ValueDescription(
      "File1.scala File2.scala"
    ) @ExtraName("f") files: List[String] = List.empty[String],
    @HelpMessage(
      "If true, writes changes to files instead of printing to stdout."
    ) @ExtraName("i") inPlace: Boolean = false,
    @HelpMessage(
      """Regex that is passed as first argument to
        |        fileToFix.replaceAll(outFrom, outTo).""".stripMargin
    ) @ValueDescription("/shared/") outFrom: String = "",
    @HelpMessage(
      """Replacement string that is passed as second argument to
        |        fileToFix.replaceAll(outFrom, outTo)""".stripMargin
    ) @ValueDescription("/custom/") outTo: String = "",
    @HelpMessage(
      "If true, run on single thread. If false (default), use all available cores."
    ) singleThread: Boolean = false,
    @HelpMessage(
      "If true, prints out debug information."
    ) debug: Boolean = false,
    @Recurse common: CommonOptions = CommonOptions()
) {

  lazy val absoluteFiles: List[File] = files.map { f =>
    val file = new File(f)
    if (file.isAbsolute) file
    else new File(new File(common.workingDirectory), f)
  }

  /** Returns ScalafixConfig from .scalafix.conf, it exists and --config was not passed. */
  lazy val resolvedConfig: Configured[ScalafixConfig] =
    for {
      mirror <- resolvedMirror
      decoder = ScalafixCompilerDecoder(mirror)
      cliArgRewrite <- rewrites
        .foldLeft(ScalafixToolbox.emptyRewrite) {
          case (rewrite, next) =>
            rewrite
              .product(decoder.read(Conf.Str(next)))
              .map { case (a, b) => a.andThen(b) }
        }
      scalafixConfig <- config match {
        case None =>
          ScalafixConfig
            .auto(common.workingDirectoryFile, mirror)(decoder)
            .getOrElse(Ok(ScalafixConfig.default))
        case Some(x) =>
          if (new File(x).isFile)
            ScalafixConfig.fromFile(new File(x), mirror)(decoder)
          else ScalafixConfig.fromString(x, mirror)(decoder)
      }
    } yield
      scalafixConfig.copy(
        rewrite = cliArgRewrite,
        reporter = scalafixConfig.reporter match {
          case r: PrintStreamReporter => r.copy(outStream = common.err)
          case _ => ScalafixReporter.default.copy(outStream = common.err)
        }
      )

  lazy val resolvedSbtConfig: Configured[ScalafixConfig] =
    resolvedConfig.map(_.copy(dialect = dialects.Sbt0137))

  lazy val resolvedMirror: Configured[Option[ScalafixMirror]] =
    (classpath, sourcepath) match {
      case (Some(cp), Some(sp)) =>
        val tryMirror = for {
          pluginPath <- scalahostNscPluginPath.fold(
            Try(offline.Mirror.autodetectScalahostNscPluginPath))(Success(_))
          mirror <- Try(ScalafixMirror.fromMirror(Mirror(cp, sp, pluginPath)))
        } yield Option(mirror)
        tryMirror match {
          case Success(x) => Ok(x)
          case scala.util.Failure(e) => ConfError.msg(e.toString).notOk
        }
      case (None, None) => Ok(None)
      case _ =>
        ConfError
          .msg(
            "The semantic API was partially configured: both a classpath and sourcepath are required.")
          .notOk
    }

  lazy val outFromPattern: Pattern = Pattern.compile(outFrom)
  def replacePath(file: File): File =
    new File(outFromPattern.matcher(file.getPath).replaceAll(outTo))
}

object Cli {
  import ArgParserImplicits._
  private val withHelp = OptionsMessages.withHelp
  val helpMessage: String = withHelp.helpMessage +
    s"""|
        |Examples:
        |  $$ scalafix --rewrites ProcedureSyntax Code.scala # print fixed file to stdout
        |  $$ cat .scalafix.conf
        |  rewrites = [ProcedureSyntax]
        |  $$ scalafix Code.scala # Same as --rewrites ProcedureSyntax
        |  $$ scalafix -i --rewrites ProcedureSyntax Code.scala # write fixed file in-place
        |
        |Exit status codes:
        | ${ExitStatus.all.mkString("\n ")}
        |""".stripMargin
  val usageMessage: String = withHelp.usageMessage
  val default = ScalafixOptions()
  // Run this at the end of the world, calls sys.exit.
  def main(args: Array[String]): Unit = {
    sys.exit(runMain(args, CommonOptions()))
  }

  def safeHandleFile(file: File, options: ScalafixOptions): ExitStatus = {
    try handleFile(file, options)
    catch {
      case NonFatal(e) =>
        reportError(file, e, options)
        ExitStatus.UnexpectedError
    }
  }

  def reportError(file: File,
                  cause: Throwable,
                  options: ScalafixOptions): Unit = {
    options.common.err.println(
      s"""Error fixing file: $file
         |Cause: $cause""".stripMargin
    )
    cause.setStackTrace(
      cause.getStackTrace.take(options.common.stackVerbosity))
    cause.printStackTrace(options.common.err)
  }

  def parsed(code: Input, config: ScalafixConfig): Either[Failed, Tree] = {
    config.parser.apply(code, config.dialect) match {
      case Parsed.Success(ast) => Right(ast)
      case Parsed.Error(pos, msg, e) =>
        Left(Fixed.Failed(Failure.ParseError(pos, msg, e)))
    }
  }

  def handleFile(file: File, options: ScalafixOptions): ExitStatus = {
    val config =
      if (file.getAbsolutePath.endsWith(".sbt")) options.resolvedSbtConfig
      else options.resolvedConfig

    val fixed = Scalafix.fix(Input.File(file), config.get)
    fixed match {
      case Fixed.Success(code) =>
        if (options.inPlace) {
          val outFile = options.replacePath(file)
          FileOps.writeFile(outFile, code)
        } else options.common.out.write(code.getBytes)
        ExitStatus.Ok
      case Fixed.Failed(e: Failure.ParseError) =>
        if (options.absoluteFiles.exists(
              _.getAbsolutePath == file.getAbsolutePath)) {
          // Only log if user explicitly specified that file.
          options.common.err.write((e.exception.getMessage + "\n").getBytes)
        }
        ExitStatus.ParseError
      case Fixed.Failed(failure) =>
        reportError(file, failure.ex, options)
        ExitStatus.ScalafixError
    }
  }

  def runOn(config: ScalafixOptions): ExitStatus = {
    val codes = config.files.map { pathStr =>
      val path = new File(pathStr)
      val workingDirectory = new File(config.common.workingDirectory)
      val realPath: File =
        if (path.isAbsolute) path
        else new File(config.common.workingDirectory, path.getPath)
      if (realPath.isDirectory) {
        val filesToFix: GenSeq[String] = {
          val files =
            FileOps
              .listFiles(realPath)
              .filter(x => x.endsWith(".scala") || x.endsWith(".sbt"))
          if (config.singleThread) files
          else files.par
        }
        val logger = new TermDisplay(new OutputStreamWriter(System.out))
        logger.init()
        val msg = "Running scalafix..."
        logger.startTask(msg, workingDirectory)
        logger.taskLength(msg, filesToFix.length, 0)
        val counter = new AtomicInteger()
        val codes = filesToFix.map { x =>
          val code = safeHandleFile(new File(x), config)
          val progress = counter.incrementAndGet()
          logger.taskProgress(msg, progress)
          code
        }
        logger.stop()
        codes.reduce(ExitStatus.merge)
      } else {
        safeHandleFile(realPath, config)
      }
    }
    codes.reduce(ExitStatus.merge)
  }

  def parse(args: Seq[String]): Either[String, WithHelp[ScalafixOptions]] =
    OptionsParser.withHelp.detailedParse(args) match {
      case Right((help, extraFiles, ls)) =>
        val configured = for {
          _ <- help.base.resolvedMirror // validate
          _ <- help.base.resolvedConfig // validate
        } yield help.map(_.copy(files = help.base.files ++ extraFiles))
        configured.toEither.left.map(_.toString())
      case Left(x) => Left(x)
    }

  def runMain(args: Seq[String], commonOptions: CommonOptions): Int = {
    parse(args) match {
      case Right(WithHelp(usage @ true, _, _)) =>
        commonOptions.out.println(usageMessage)
        0
      case Right(WithHelp(_, help @ true, _)) =>
        commonOptions.out.println(helpMessage)
        0
      case Right(WithHelp(_, _, options)) =>
        runOn(options.copy(common = commonOptions)).code
      case Left(error) =>
        commonOptions.err.println(error)
        1
    }
  }

  def nailMain(nGContext: NGContext): Unit = {
    val code =
      runMain(
        nGContext.getArgs,
        CommonOptions(
          workingDirectory = nGContext.getWorkingDirectory,
          out = nGContext.out,
          in = nGContext.in,
          err = nGContext.err
        )
      )
    nGContext.exit(code)
  }
}
