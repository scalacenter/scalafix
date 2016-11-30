package scalafix.nsc

import scala.meta.Defn
import scala.meta.Type
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.rewrite.SemanticApi

class ScalafixNsc(val global: Global) extends Plugin {
  val name = "scalafix"
  val description = "Fix stuff"
  val components: List[PluginComponent] =
    new ScalafixNscComponent(this, global) :: Nil
}

trait ReflectToolkit {
  val global: scala.tools.nsc.Global
  lazy val g: global.type = global
}

trait NscSemanticApi extends ReflectToolkit {
  private def getSemanticApi(unit: g.CompilationUnit): SemanticApi = {
    new SemanticApi {
      override def typeSignature(defn: Defn): Option[Type] = ???
    }
  }

  def fix(unit: g.CompilationUnit): Fixed = {
    val api = getSemanticApi(unit)
    val input = m.Input.File(unit.source.file.file)
    Scalafix.fix(input, ScalafixConfig(), Some(api))
  }
}

class ScalafixNscComponent(plugin: Plugin, val global: Global)
    extends PluginComponent
    with ReflectToolkit
    with NscSemanticApi {

  override val phaseName: String = "scalafix"
  override val runsAfter: List[String] = "typer" :: Nil
  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def name: String = "scalafix"
    override def run(): Unit = {
      global.currentRun.units.foreach { unit =>
        val fixed = fix(unit)
        println(fixed)
        println(unit)
      }
    }
  }
}
