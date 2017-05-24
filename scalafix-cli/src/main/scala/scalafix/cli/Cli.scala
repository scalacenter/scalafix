package scalafix.cli

import scala.collection.immutable.Seq
import scala.meta.Database
import scala.meta.io.AbsolutePath
import scalafix.cli.CliCommand.PrintAndExit
import scalafix.cli.CliCommand.RunScalafix
import caseapp.core.WithHelp
import com.martiansoftware.nailgun.NGContext
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

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

  class NonZeroExitCode(exitStatus: ExitStatus)
      extends Exception(s"Got exit code $exitStatus")
  object NonZeroExitCode {
    def apply(exitStatus: ExitStatus): NonZeroExitCode = {
      val err = new NonZeroExitCode(exitStatus)
      err.setStackTrace(Array.empty)
      err
    }
  }
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
