package scalafix.nsc

import scala.reflect.internal.util.NoPosition
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.util.control.NonFatal
import scalafix.Fixed
import scalafix.ScalafixConfig
import scalafix.util.FileOps
import scalafix.util.logger

class ScalafixNscComponent(plugin: Plugin,
                           val global: Global,
                           getConfig: () => ScalafixConfig)
    extends PluginComponent
    with ReflectToolkit
    with NscSemanticApi {
  override val phaseName: String = "scalafix"
  override val runsAfter: List[String] = "typer" :: Nil

  private def runOn(unit: g.CompilationUnit): Unit = {
    if (unit.source.file.exists &&
        unit.source.file.file.isFile &&
        !unit.isJava) {
      logger.elem(getConfig())
      fix(unit, getConfig()) match {
        case Fixed.Success(fixed) =>
          if (fixed.nonEmpty && fixed != new String(unit.source.content)) {
            FileOps.writeFile(unit.source.file.file, fixed)
          }
        case Fixed.Failed(e) =>
          g.reporter.warning(
            unit.body.pos,
            "Failed to run scalafix. " + e.getMessage
          )
      }
    }
  }
  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def name: String = "scalafix"
    override def run(): Unit = {
      global.currentRun.units.foreach { unit =>
        try {
          runOn(unit)
        } catch {
          case NonFatal(e) =>
            global.reporter.warning(
              NoPosition,
              s"Failed to fix ${unit.source}. Error: ${e.getMessage}. $e")
        }
      }
    }
  }
}
