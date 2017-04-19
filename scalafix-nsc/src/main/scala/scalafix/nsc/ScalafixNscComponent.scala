package scalafix
package nsc

import scala.meta.internal.scalahost.v1.online.Mirror
import scala.meta.semantic.v1
import scala.meta.semantic.v1._
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.util.control.NonFatal
import scalafix.Failure.ParseError
import scalafix.config.ScalafixConfig
import scalafix.rewrite.ScalafixMirror
import scalafix.util.FileOps

class ScalafixNscComponent(plugin: Plugin,
                           val global: Global,
                           getConfig: () => ScalafixConfig,
                           getRewrite: ScalafixMirror => Rewrite)
    extends PluginComponent
    with ReflectToolkit
    with NscScalafixMirror {
  // warnUnusedImports could be set triggering a compiler error
  // if fatal warnings is also enabled.
  if (getConfig().imports.removeUnused) {
    g.settings.warnUnusedImport.tryToSetFromPropertyValue("true")
    if (g.settings.fatalWarnings.value) {
      val msg =
        "-Xfatal-warnings is enabled along with imports.removeUnused=true in scalafix. " +
          "Consider disabling -Xfatal-warnings to avoid compilation errors."
      getConfig().reporter.warn(msg)
    }
    g.settings.fatalWarnings.tryToSetFromPropertyValue("false")
  }

  override val phaseName: String = "scalafix"
  override val runsRightAfter: Option[String] = Some("typer")
  override val runsAfter: List[String] = Nil

  private def runOn(unit: g.CompilationUnit)(implicit mirror: Mirror): Unit = {
    if (unit.source.file.exists &&
        unit.source.file.file.isFile &&
        !unit.isJava) {
      val config = getConfig()
      val fixed = fix(unit, config, getRewrite).get
      if (fixed.nonEmpty && fixed != new String(unit.source.content)) {
        FileOps.writeFile(unit.source.file.file, fixed)
      }
    }
  }
  override def newPhase(prev: Phase): Phase = new Phase(prev) {
    override def name: String = "scalafix"
    override def run(): Unit = {
      implicit val mirror = new Mirror(global) {
        override lazy val database = super.database
      }
      global.currentRun.units.foreach { unit =>
        try {
          runOn(unit)
        } catch {
          case NonFatal(e) if !e.isInstanceOf[ParseError] =>
            val config = getConfig()
            val err: (String) => Unit =
              if (config.fatalWarnings)
                (msg: String) => g.reporter.error(unit.body.pos, msg)
              else
                (msg: String) =>
                  g.reporter.info(unit.body.pos, msg, force = true)
            val details =
              if (config.fatalWarnings)
                e.getStackTrace.mkString("\n", "\n", "")
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
