package scalafix.cli

import com.martiansoftware.nailgun.NGContext
import scalafix.internal.v1.MainOps

object Cli {
  def helpMessage: String = MainOps.helpMessage(80)
  def main(args: Array[String]): Unit = {
    scalafix.v1.Main.main(args)
  }
  def nailMain(nGContext: NGContext): Unit = {
    scalafix.v1.Main.nailMain(nGContext)
  }
}
