package scalafix.nsc

import scala.meta.Defn
import scala.meta.Type
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

class ScalafixNsc(val global: Global) extends Plugin {
  val name = "scalafix"
  val description = "Refactoring tool."
  val components: List[PluginComponent] =
    new ScalafixNscComponent(this, global) :: Nil
}
