package scalafix.tests.util.compat

import java.nio.file.Path

import dotty.tools.dotc.Main

object CompatSemanticdb {

  def scalacOptions(src: Path): Array[String] = {
    Array[String](
      "-Xsemanticdb",
      s"-sourceroot:$src"
    )
  }

  def runScalac(scalacOptions: Seq[String]): Unit = {
    Main.process(scalacOptions.toArray)
  }
}
