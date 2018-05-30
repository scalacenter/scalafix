package scalafix.v1

import com.martiansoftware.nailgun.NGContext
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import metaconfig.Conf
import metaconfig.Configured
import scala.meta.io.AbsolutePath
import scalafix.Versions
import scalafix.cli.ExitStatus
import scalafix.internal.config.ScalafixReporter
import scalafix.internal.v1._

object Main {

  def nailMain(nGContext: NGContext): Unit = {
    val exit = run(
      nGContext.getArgs,
      Paths.get(nGContext.getWorkingDirectory),
      nGContext.out
    )
    nGContext.exit(exit.code)
  }

  def main(args: Array[String]): Unit = {
    val exit = run(args, AbsolutePath.workingDirectory.toNIO, System.out)
    if (!args.contains("--no-sys-exit")) {
      sys.exit(exit.code)
    } else if (!exit.isOk) {
      throw new NonZeroExitCode(exit)
    }
  }

  def run(args: Array[String], cwd: Path, out: PrintStream): ExitStatus =
    Conf
      .parseCliArgs[Args](args.toList)
      .andThen(c => c.as[Args](Args.decoder(AbsolutePath(cwd), out)))
      .andThen(_.validate) match {
      case Configured.Ok(validated) =>
        if (validated.args.help) {
          MainOps.helpMessage(out, 80)
          ExitStatus.Ok
        } else if (validated.args.version) {
          out.println(Versions.version)
          ExitStatus.Ok
        } else if (validated.args.bash) {
          out.println(CompletionsOps.bashCompletions)
          ExitStatus.Ok
        } else if (validated.args.zsh) {
          out.println(CompletionsOps.zshCompletions)
          ExitStatus.Ok
        } else if (validated.rules.isEmpty) {
          out.println("Missing --rules")
          ExitStatus.CommandLineError
        } else {
          val adjusted = validated.copy(
            args = validated.args.copy(
              out = out,
              cwd = AbsolutePath(cwd)
            )
          )
          MainOps.run(adjusted)
        }
      case Configured.NotOk(err) =>
        ScalafixReporter.default.copy(outStream = out).error(err.toString())
        ExitStatus.CommandLineError
    }
}
