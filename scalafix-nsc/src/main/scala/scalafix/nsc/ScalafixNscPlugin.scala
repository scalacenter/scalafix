package scalafix.nsc

import scala.meta.Defn
import scala.meta.Type
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scalafix.ScalafixConfig
import scalafix.rewrite.Rewrite

class ScalafixNscPlugin(val global: Global) extends Plugin {
  val name = "scalafix"
  val description = "Refactoring tool."
  var config: ScalafixConfig = ScalafixConfig(rewrites = Rewrite.allRewrites)
  val components: List[PluginComponent] =
    new ScalafixNscComponent(this, global, () => config) :: Nil

  override def init(options: List[String], error: (String) => Unit): Boolean = {
    ScalafixConfig.fromNames(options) match {
      case Left(msg) =>
        error(msg)
        false
      case Right(userConfig) =>
        config = userConfig
        true
    }
  }
}
