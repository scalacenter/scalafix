package scalafix

import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

class ScalafixCompilerPlugin(val global: Global) extends Plugin {
  val name = "scalafix"
  val description = "Fix stuff"
  val components: List[PluginComponent] = new ScalafixComponent(this, global) :: Nil
}

class ScalafixComponent(plugin: Plugin, val global: Global)
    extends PluginComponent {
  override val phaseName: String = "scalafix"
  override val runsAfter: List[String] = "typer" :: Nil
  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def name: String = "scalafix"
    override def run(): Unit = {
      global.currentRun.units.foreach { unit =>
        println(unit)
      }
    }
  }
}
