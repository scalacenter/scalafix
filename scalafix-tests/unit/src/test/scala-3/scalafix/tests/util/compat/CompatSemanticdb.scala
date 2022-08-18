package scalafix.tests.util.compat

import java.nio.file.Path
import dotty.tools.dotc.Main

object CompatSemanticdb {

  def scalacOptions(src: Path, target: Path): Array[String] = {
    Array[String](
      s"-semanticdb-target:$target"
    ) ++ scalacOptions(src)
  }

  def scalacOptions(src: Path): Array[String] = {
    Array[String](
      "-Xsemanticdb",
      "-Xplugin-require:semanticdb" // ,
      // s"-P:semanticdb:sourceroot:$src"
    )
  }

  def runScalac(scalacOptions: Seq[String]): Unit = {
    Main.process(scalacOptions.toArray)
  }
}
