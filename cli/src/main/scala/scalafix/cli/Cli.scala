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
import scalafix.config.ProjectFiles
import scalafix.config.ScalafixConfig

import caseapp._
import caseapp.core.Messages
import com.martiansoftware.nailgun.NGContext

case class CommonOptions(
    @Hidden workingDirectory: String = System.getProperty("user.dir"),
    @Hidden out: PrintStream = System.out,
    @Hidden in: InputStream = System.in,
    @Hidden err: PrintStream = System.err
)

@AppName("scalafix")
@AppVersion(scalafix.Versions.nightly)
@ProgName("scalafix")
case class ScalafixOptions(
    @HelpMessage(
      s"Rules to run, one of: ${Rewrite.default.mkString(", ")}"
    ) rewrites: List[Rewrite] = Rewrite.default.toList,
    @Hidden @HelpMessage(
      "Files to fix. Runs on all *.scala files if given a directory."
    ) @ExtraName("f") files: List[String] = Nil,
    @HelpMessage(
      "Target directory with .class files that contain TASTY fields"
    ) @ExtraName("f") target: List[String] = Nil,
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
) extends App {
  def toConfig = ScalafixConfig(
    rewrites = rewrites,
    project = ProjectFiles(
      targetFiles = target.map(new File(_))
    )
  )
  Cli.runOn(this)
}

object Cli extends AppOf[ScalafixOptions] {
  val helpMessage: String = Messages[ScalafixOptions].withHelp.helpMessage

  val default = ScalafixOptions()

  def safeHandleFile(file: File,
                     options: ScalafixOptions,
                     config: ScalafixConfig): Unit = {
    try handleFile(file, options, config)
    catch {
      case NonFatal(e) =>
        options.common.err.println(s"""Unexpected error fixing file: $file
                                      |Cause: $e""".stripMargin)
    }
  }
  def handleFile(file: File,
                 options: ScalafixOptions,
                 config: ScalafixConfig): Unit = {
    val fixed = Scalafix.fix(FileOps.readFile(file), options.toConfig)
    fixed match {
      case Right(code) =>
        if (options.inPlace) {
          FileOps.writeFile(file, code)
        } else options.common.out.write(code.getBytes)
      case Left(e: Failure.ParseError) =>
        if (options.files.contains(file.getPath)) {
          // Only log if user explicitly specified that file.
          options.common.err.write(e.toString.getBytes())
        }
      case Left(e) =>
        options.common.err.write(s"Failed to fix $file. Cause: $e".getBytes)
    }
  }

  def runOn(options: ScalafixOptions): Unit = {
    val config = options.toConfig
    options.files.foreach { pathStr =>
      val path = new File(pathStr)
      val workingDirectory = new File(options.common.workingDirectory)
      val realPath: File =
        if (path.isAbsolute) path
        else new File(options.common.workingDirectory, path.getPath)
      if (realPath.isDirectory) {
        val filesToFix: GenSeq[String] = {
          val files =
            FileOps.listFiles(realPath).filter(x => x.endsWith(".scala"))
          if (options.parallel) files.par
          else files
        }
        val logger = new TermDisplay(new OutputStreamWriter(System.out))
        logger.init()
        val msg = "Running scalafix..."
        logger.startTask(msg, workingDirectory)
        logger.taskLength(msg, filesToFix.length, 0)
        val counter = new AtomicInteger()
        filesToFix.foreach { x =>
          safeHandleFile(new File(x), options, config)
          val progress = counter.incrementAndGet()
          logger.taskProgress(msg, progress)
        }
        logger.stop()
      } else {
        safeHandleFile(realPath, options, config)
      }
    }
  }

  def parse(args: Seq[String]): Either[String, ScalafixOptions] =
    CaseApp.parse[ScalafixOptions](args) match {
      case Right((config, extraFiles)) =>
        Right(config.copy(files = config.files ++ extraFiles))
      case Left(x) => Left(x)
    }

  def runMain(args: Seq[String], commonOptions: CommonOptions): Unit = {
    parse(args) match {
      case Right(options) =>
        runOn(options.copy(common = commonOptions))
      case Left(error) =>
        commonOptions.err.println(error)
        System.exit(1)
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

object Cli210 {
  def main(args: Array[String]): Unit = {
    Cli.runMain(args, CommonOptions())
  }
}
