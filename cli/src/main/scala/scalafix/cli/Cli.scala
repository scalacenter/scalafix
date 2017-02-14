package scalafix.cli

import scala.collection.GenSeq
import scalafix.Failure
import scalafix.Scalafix
import scalafix.rewrite.Rewrite
import scalafix.util.FileOps

import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.PrintStream
import java.util.concurrent.atomic.AtomicInteger
import ArgParserImplicits._
import scala.util.control.NonFatal
import scalafix.Fixed
import scalafix.cli.termdisplay.TermDisplay
import scalafix.rewrite.ScalafixRewrite
import scalafix.rewrite.ScalafixRewrites
import scalafix.config.ScalafixConfig

import caseapp._
import caseapp.core.Messages
import caseapp.core.WithHelp
import com.martiansoftware.nailgun.NGContext

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err
)

@AppName("scalafix")
@AppVersion(scalafix.Versions.version)
@ProgName("scalafix")
case class ScalafixOptions(
    @HelpMessage(
      s"Rules to run, one of: ${ScalafixRewrites.all.mkString(", ")}"
    ) rewrites: List[ScalafixRewrite] = ScalafixRewrites.syntax.toList,
    @Hidden @HelpMessage(
      "Files to fix. Runs on all *.scala files if given a directory."
    ) @ExtraName("f") files: List[String] = List.empty[String],
    @HelpMessage(
      "If true, writes changes to files instead of printing to stdout."
    ) @ExtraName("i") inPlace: Boolean = false,
    @HelpMessage(
      "If true, uses all available CPUs. If false, runs in single thread."
    ) parallel: Boolean = true,
    @HelpMessage(
      "If true, prints out debug information."
    ) debug: Boolean = false,
    @Recurse common: CommonOptions = CommonOptions()
)

object Cli {
  private val withHelp = Messages[ScalafixOptions].withHelp
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
  def handleFile(file: File, config: ScalafixOptions): Unit = {
    val fixed =
      Scalafix.fix(FileOps.readFile(file), ScalafixConfig(config.rewrites))
    fixed match {
      case Fixed.Success(code) =>
        if (config.inPlace) {
          FileOps.writeFile(file, code)
        } else config.common.out.write(code.getBytes)
      case Fixed.Failed(e: Failure.ParseError) =>
        if (config.files.contains(file.getPath)) {
          // Only log if user explicitly specified that file.
          config.common.err.write(e.toString.getBytes())
        }
      case Fixed.Failed(e) =>
        config.common.err.write(s"Failed to fix $file. Cause: $e".getBytes)
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
          if (config.parallel) files.par
          else files
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
    Parser[ScalafixOptions].withHelp.detailedParse(args) match {
      case Right((help, extraFiles, _)) =>
        Right(help.map(_.copy(files = help.base.files ++ extraFiles)))
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
