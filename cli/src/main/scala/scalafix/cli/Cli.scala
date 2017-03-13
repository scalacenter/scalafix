package scalafix.cli

import scala.collection.GenSeq
import scala.meta.inputs.Input
import scala.util.control.NonFatal
import scalafix.Failure
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.cli.ArgParserImplicits._
import scalafix.cli.termdisplay.TermDisplay
import scalafix.config.ScalafixConfig
import scalafix.rewrite.ProcedureSyntax
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
import caseapp.core.Messages
import caseapp.core.WithHelp
import com.martiansoftware.nailgun.NGContext
import org.scalameta.logger

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err
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
    case None => ScalafixConfig.auto(common.workingDirectoryFile)
    case Some(x) => x
  }

  lazy val outFromPattern: Pattern = Pattern.compile(outFrom)
  def replacePath(file: File): File =
    new File(outFromPattern.matcher(file.getPath).replaceAll(outTo))
}

object Cli {
  import ArgParserImplicits._
  private val withHelp = OptionsMessages.withHelp
  val helpMessage: String = withHelp.helpMessage
  val usageMessage: String = withHelp.usageMessage
  val default = ScalafixOptions()
  // Run this at the end of the world, calls sys.exit.
  def main(args: Array[String]): Unit = {
    sys.exit(runMain(args, CommonOptions()))
  }

  def safeHandleFile(file: File, config: ScalafixOptions): Unit = {
    try handleFile(file, config)
    catch {
      case NonFatal(e) =>
        config.common.err.println(s"""Unexpected error fixing file: $file
                                     |Cause: $e""".stripMargin)
    }
  }

  def handleFile(file: File, options: ScalafixOptions): Unit = {
    val fixed = Scalafix.fix(Input.File(file), options.resolvedConfig)
    fixed match {
      case Fixed.Success(code) =>
        if (options.inPlace) {
          val outFile = options.replacePath(file)
          FileOps.writeFile(outFile, code)
        } else options.common.out.write(code.getBytes)
      case Fixed.Failed(e: Failure.ParseError) =>
        if (options.files.contains(file.getPath)) {
          // Only log if user explicitly specified that file.
          options.common.err.write(e.toString.getBytes())
        }
      case Fixed.Failed(e) =>
        options.common.err.write(s"Failed to fix $file. Cause: $e".getBytes)
    }
  }

  def runOn(config: ScalafixOptions): Int = {
    config.files.foreach { pathStr =>
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
        filesToFix.foreach { x =>
          safeHandleFile(new File(x), config)
          val progress = counter.incrementAndGet()
          logger.taskProgress(msg, progress)
        }
        logger.stop()
      } else {
        safeHandleFile(realPath, config)
      }
    }
    0
  }

  def parse(args: Seq[String]): Either[String, WithHelp[ScalafixOptions]] =
    OptionsParser.withHelp.detailedParse(args) match {
      case Right((help, extraFiles, ls)) =>
        Right(
          help.map(c =>
            c.copy(
              files = help.base.files ++ extraFiles,
              rewrites = c.rewrites ++ c.config.map(_.rewrites).getOrElse(Nil)
          ))
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
        runOn(options.copy(common = commonOptions))
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
