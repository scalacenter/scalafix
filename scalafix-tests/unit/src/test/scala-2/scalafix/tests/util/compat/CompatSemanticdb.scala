package scalafix.tests.util.compat

import java.nio.file.Path

import scala.tools.nsc.Main

object CompatSemanticdb {

  def scalacOptions(src: Path, target: Path): Array[String] = {
    Array[String](
      s"-P:semanticdb:targetroot:$target"
    ) ++ scalacOptions(src)
  }

  def scalacOptions(src: Path): Array[String] = {
    Array[String](
      s"-Xplugin:${SemanticdbPlugin.semanticdbPluginPath()}",
      "-Xplugin-require:semanticdb",
      s"-P:semanticdb:sourceroot:$src"
    )
  }

  def runScalac(scalacOptions: Seq[String]): Unit = {
    Main.process(scalacOptions.toArray)
  }
}
