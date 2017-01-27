package scalafix.nsc

import scala.collection.mutable
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.typechecker.Contexts
import scala.util.control.NonFatal
import scalafix.Failure.ParseError
import scalafix.ScalafixConfig
import scalafix.util.FileOps
import scalafix.util.logger

class ScalafixNscComponent(plugin: Plugin,
                           val global: Global,
                           getConfig: () => ScalafixConfig)
    extends PluginComponent
    with ReflectToolkit
    with HijackImportInfos
    with NscSemanticApi {

  this.hijackImportInfos()
  // warnUnusedImports could be set triggering a compiler error
  // if fatal warnings is also enabled.
  g.settings.warnUnusedImport.tryToSetFromPropertyValue("true")
  g.settings.fatalWarnings.tryToSetFromPropertyValue("false")

  override val phaseName: String = "scalafix"
  override val runsAfter: List[String] = "typer" :: Nil

  private def runOn(unit: g.CompilationUnit): Unit = {
    if (unit.source.file.exists &&
        unit.source.file.file.isFile &&
        !unit.isJava) {
      val config = getConfig()
      val fixed = fix(unit, config).get
      if (fixed.nonEmpty && fixed != new String(unit.source.content)) {
        FileOps.writeFile(unit.source.file.file, fixed)
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
          case NonFatal(e) if !e.isInstanceOf[ParseError] =>
            val config = getConfig()
            val err: (String) => Unit =
              if (config.fatalWarning)
                (msg: String) => g.reporter.error(unit.body.pos, msg)
              else
                (msg: String) =>
                  g.reporter.info(unit.body.pos, msg, force = true)
            val details =
              if (config.fatalWarning) e.getStackTrace.mkString("\n", "\n", "")
              else ""
            err(
              s"Failed to fix ${unit.source}. Error: ${e.getMessage}. $e" +
                details
            )
        }
      }
    }
  }
}
