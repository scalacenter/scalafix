package scalafix.nsc

import scala.meta.internal.scalahost.v1.online.Mirror
import scala.meta.semantic.v1
import scala.meta.semantic.v1._
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
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
    with NscScalafixMirror {
  // warnUnusedImports could be set triggering a compiler error
  // if fatal warnings is also enabled.
  g.settings.warnUnusedImport.tryToSetFromPropertyValue("true")
  g.settings.fatalWarnings.tryToSetFromPropertyValue("false")

  override val phaseName: String = "scalafix"
  override val runsRightAfter: Option[String] = Some("typer")
  override val runsAfter: List[String] = Nil

  private def runOn(unit: g.CompilationUnit)(implicit mirror: Mirror): Unit = {
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
      implicit val mirror = new Mirror(global)
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
