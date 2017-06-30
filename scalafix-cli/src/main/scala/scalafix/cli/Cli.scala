package scalafix.cli

import java.nio.file.Path
import scala.collection.immutable.Seq
import scala.meta.Database
import scala.meta.io.AbsolutePath
import scalafix.cli.CliCommand.PrintAndExit
import scalafix.cli.CliCommand.RunScalafix
import scalafix.internal.cli.CommonOptions
import scalafix.internal.cli.ScalafixOptions
import scalafix.rewrite.ScalafixRewrites
import caseapp.Name
import caseapp.core.Arg
import caseapp.core.Messages
import caseapp.core.WithHelp
import com.martiansoftware.nailgun.NGContext
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

object Cli {
  import scalafix.internal.cli.ArgParserImplicits._
  private val withHelp: Messages[WithHelp[ScalafixOptions]] =
    OptionsMessages.copy(optionsDesc = "[options] [<file>...]").withHelp
  val helpMessage: String = withHelp.helpMessage +
    s"""|Available rewrites: ${ScalafixRewrites.allNames.mkString(", ")}
        |
        |NOTE. The command line tool is mostly intended to be invoked programmatically
        |from build-tool integrations such as sbt-scalafix. The necessary fixture to run
        |semantic rewrites is tricky to setup manually.
        |
        |Scalafix chooses which files to fix according to the following rules:
        |- when --syntactic is passed, then Scalafix looks for .scala files in the provided files/directories.
        |- by default, looks for .semanticdb files and matches them back to the original files.
        |  - if --classpath and --sourceroot are provided, then those are used to find .semanticdb files.
        |  - otherwise, Scalafix will automatically look for META-INF/semanticdb directories from the
        |    current working directory.
        |
        |Examples (semantic):
        |  $$ scalafix # automatically finds .semanticdb files and runs rewrite configured in .scalafix.conf.
        |  $$ scalafix <directory> # same as above except only run on files in <directory>
        |  $$ scalafix --rewrites RemoveUnusedImports # same as above but run RemoveUnusedImports.
        |  $$ scalafix --classpath <foo.jar:target/classes> # explicitly pass classpath, --sourceroot is cwd.
        |  $$ scalafix --classpath <foo.jar:target/classes> --sourceroot <directory>
        |
        |Examples (syntactic):
        |  $$ scalafix --syntactic --rewrites=ProcedureSyntax Code.scala # write fixed file in-place
        |  $$ scalafix --syntactic --rewrites=ProcedureSyntax --stdout Code.scala # print fixed file to stdout
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

  def toZshOption(arg: Arg): scala.Seq[String] = {
    if (arg.noHelp) Nil
    else {
      // See https://github.com/zsh-users/zsh-completions/blob/master/zsh-completions-howto.org#writing-completion-functions-using-_arguments
      // for more details on how to use _arguments in zsh.
      import caseapp.core.NameOps
      val (repeat, assign, message, action) = arg.name match {
        case "rewrites" => ("*", "=", ":rewrite", ":_rewrite_names")
        case _ => ("", "", "", "")
      }
      val description = arg.helpMessage
        .map { x =>
          val escaped = x.message
            .replaceAll("\n *", " ")
            .replaceAllLiterally(":", "\\:")
          s"$assign[$escaped]"
        }
        .getOrElse("")
      (Name(arg.name) +: arg.extraNames).distinct.map { name =>
        s""""$repeat${name.option}$description$message$action""""
      }
    }
  }

  def bashArgs: String = {
    import caseapp.core.NameOps
    withHelp.args
      .flatMap(arg => caseapp.Name(arg.name) +: arg.extraNames)
      .map(_.option)
      .distinct
      .mkString(" ")
  }

  def zshArgs: String = {
    withHelp.args.flatMap(toZshOption).mkString(" \\\n   ")
  }

  def rewriteNames: String =
    ScalafixRewrites.allNames.map(x => s""""$x"""").mkString(" \\\n  ")

  def bashCompletions: String =
    s"""
_scalafix()
{
    local cur prev opts
    COMPREPLY=()
    cur="$${COMP_WORDS[COMP_CWORD]}"
    prev="$${COMP_WORDS[COMP_CWORD-1]}"
    rewrites="${ScalafixRewrites.allNames.mkString(" ")}"
    opts="$bashArgs"

    case "$${prev}" in
      --rewrites|-r )
        COMPREPLY=(   $$(compgen -W "$${rewrites}" -- $${cur}) )
        return 0
        ;;
    esac
    if [[ $${cur} == -* ]] ; then
        COMPREPLY=(   $$(compgen -W "$${opts}" -- $${cur}) )
        return 0
    fi
}
complete -F _scalafix scalafix
"""

  def zshCompletions: String = {
    s"""#compdef scalafix
typeset -A opt_args
local context state line

_rewrite_names () {
   compadd $rewriteNames
}

local -a scalafix_opts
scalafix_opts=(
  $zshArgs
)

case $$words[$$CURRENT] in
      *) _arguments $$scalafix_opts "*::filename:_files";;
esac

return 0
"""
  }

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

  def isScalaPath(path: Path): Boolean = {
    val filename = path.getFileName.toString
    filename.endsWith(".scala") || filename.endsWith(".sbt")
  }

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
      case Right((WithHelp(_, _, options), _, _)) if options.bash =>
        PrintAndExit(bashCompletions, ExitStatus.Ok)
      case Right((WithHelp(_, _, options), _, _)) if options.zsh =>
        PrintAndExit(zshCompletions, ExitStatus.Ok)
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
        if (exit.isOk) commonOptions.out.println(msg)
        else commonOptions.reporter.error(msg)
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
