package scalafix.nsc

import scala.meta.Defn
import scala.meta.Type
import scala.meta.internal.scalahost.ScalahostPlugin
import scala.meta.internal.scalahost.v1.online.Mirror
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scalafix.ScalafixConfig

import java.io.File

class ScalafixNscPlugin(val global: Global) extends Plugin {
  private val scalafixComponent =
    new ScalafixNscComponent(this, global, () => config)
  // IMPORTANT. This needs to happen before we create ScalahostPlugin in order to hijack the
  // original global.analyzer instead of the Scalahost hijacked analyzer.
  // It seems warn-unused-imports still uses the old g.analyzer to collect import infos.
  scalafixComponent.hijackImportInfos()
  private val scalahostPlugin = new ScalahostPlugin(global) // let scalahost hijack global
  global.settings.plugin.appendToValue("scalahost")
  val mirror = new Mirror(global)
  val name = "scalafix"
  val description = "Refactoring tool."
  var config: ScalafixConfig = ScalafixConfig()
  val component: ScalafixNscComponent = scalafixComponent
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
