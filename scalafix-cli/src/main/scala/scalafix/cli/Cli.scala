package scalafix.cli

import com.martiansoftware.nailgun.NGContext

object Cli {
  def main(args: Array[String]): Unit = {
    scalafix.v1.Main.main(args)
  }
  def nailMain(nGContext: NGContext): Unit = {
    scalafix.v1.Main.nailMain(nGContext)
  }
}
