package scalafix.v1

import com.martiansoftware.nailgun.NGContext
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import scala.meta.io.AbsolutePath
import scalafix.cli.ExitStatus
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

  def run(args: Array[String], cwd: Path, out: PrintStream): ExitStatus = {
    MainOps.run(args, Args.default(AbsolutePath(cwd), out))
  }

}
