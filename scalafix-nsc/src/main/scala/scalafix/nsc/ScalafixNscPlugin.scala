package scalafix.nsc

import scala.meta.Defn
import scala.meta.Type
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scalafix.ScalafixConfig

import java.io.File

class ScalafixNscPlugin(val global: Global) extends Plugin {
  val name = "scalafix"
  val description = "Refactoring tool."
  var config: ScalafixConfig = ScalafixConfig()
  val component: ScalafixNscComponent =
    new ScalafixNscComponent(this, global, () => config)
  val components: List[PluginComponent] = component :: Nil

  override def init(options: List[String], error: (String) => Unit): Boolean = {
    options match {
      case Nil => true
      case file :: Nil =>
        ScalafixConfig.fromFile(new File(file)) match {
          case Left(msg) =>
            error(msg)
            false
          case Right(userConfig) =>
            config = userConfig
            true
        }
      case els =>
        super.init(els, error)
    }
  }
}
