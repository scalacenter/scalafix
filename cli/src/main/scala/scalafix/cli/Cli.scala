package scalafix.cli

import scala.collection.GenSeq
import scala.meta.inputs.Input
import scala.util.control.NonFatal
import scalafix.Failure
import scalafix.syntax._
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config.ScalafixConfig
import scalafix.rewrite.ProcedureSyntax
import scalafix.rewrite.ScalafixMirror
import scalafix.rewrite.ScalafixRewrite
import scalafix.rewrite.ScalafixRewrites
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
    ) @ExtraName("c") config: Option[ScalafixConfig] = None,
    @HelpMessage(
      "java.io.File.pathSeparator separated list of jar files" +
        "or directories containing classfiles and `semanticdb` files." +
        "The `semanticdb` files are emitted by the scalahost-nsc" +
        "compiler plugin and are necessary for the semantic API to" +
        "function. The classfiles + jar files are necessary for" +
        "runtime compilation of quasiquotes when extracting" +
        """symbols (that is, `q"scala.Predef".symbol`)."""
    ) @ValueDescription(
      "entry1.jar:entry2.jar"
    ) classpath: Option[String] = None,
    @HelpMessage(
      "java.io.File.pathSeparator separated list of" +
        "Scala source files OR directories containing Scala" +
        "source files."
    ) @ValueDescription(
      "File2.scala:File1.scala:src/main/scala"
    ) sourcepath: Option[String] = None,
    @HelpMessage(
      s"Additional rewrite rules to run. NOTE. rewrite.rules = [ .. ] from --config will also run."
    ) @ValueDescription(
      s"$ProcedureSyntax OR file:LocalFile.scala OR scala:full.Name OR https://gist.com/.../Rewrite.scala"
    ) rewrites: List[ScalafixRewrite] = ScalafixRewrites.syntax,
    @HelpMessage(
      "Files to fix. Runs on all *.scala files if given a directory."
    ) @ValueDescription(
      "File1.scala File2.scala"
    ) @ExtraName("f") files: List[String] = List.empty[String],
    @HelpMessage(
      "If true, writes changes to files instead of printing to stdout."
    ) @ExtraName("i") inPlace: Boolean = false,
    @HelpMessage(
      "Regex that is passed as first argument to " +
        "fileToFix.replaceAll(outFrom, outTo)."
    ) @ValueDescription("/shared/") outFrom: String = "",
    @HelpMessage(
      "Replacement string that is passed as second " +
        "argument to fileToFix.replaceAll(outFrom, outTo)"
    ) @ValueDescription("/custom/") outTo: String = "",
    @HelpMessage(
      "If true, run on single thread. If false (default), use all available cores."
    ) singleThread: Boolean = false,
    @HelpMessage(
      "If true, prints out debug information."
    ) debug: Boolean = false,
    @Recurse common: CommonOptions = CommonOptions()
) {

  /** Returns ScalafixConfig from .scalafix.conf, it exists and --config was not passed. */
  lazy val resolvedConfig: ScalafixConfig = config match {
    case None =>
      ScalafixConfig
        .auto(common.workingDirectoryFile)
        .getOrElse(
          ScalafixConfig.default
        )
        .withRewrites(_ ++ rewrites)
    case Some(x) => x
  }

  lazy val resolvedMirror: Either[String, Option[ScalafixMirror]] =
    (classpath, sourcepath) match {
      case (Some(cp), Some(sp)) =>
        Right(Some(ScalafixMirror.fromMirror(scala.meta.Mirror(cp, sp))))
      case (None, None) => Right(None)
      case _ =>
        Left(
          "The semantic API was partially configured: both a classpath and sourcepath are required.")
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

  def handleFile(file: File, options: ScalafixOptions): ExitStatus = {
    val fixed = Scalafix.fix(Input.File(file),
                             options.resolvedConfig,
                             options.resolvedMirror.get)
    fixed match {
      case Fixed.Success(code) =>
        if (options.inPlace) {
          val outFile = options.replacePath(file)
          FileOps.writeFile(outFile, code)
        } else options.common.out.write(code.getBytes)
        ExitStatus.Ok
      case Fixed.Failed(e: Failure.ParseError) =>
        if (options.files.contains(file.getPath)) {
          // Only log if user explicitly specified that file.
          options.common.err.write(e.toString.getBytes())
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
            FileOps.listFiles(realPath).filter(x => x.endsWith(".scala"))
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
        Right(
          help.map(_.copy(files = help.base.files ++ extraFiles))
        )
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
    runMain(
      nGContext.getArgs,
      CommonOptions(
        workingDirectory = nGContext.getWorkingDirectory,
        out = nGContext.out,
        in = nGContext.in,
        err = nGContext.err
      )
    )
  }
}
