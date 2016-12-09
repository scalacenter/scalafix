package scalafix.nsc

import scala.meta.Defn
import scala.meta.Type
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scalafix.ScalafixConfig
import scalafix.rewrite.Rewrite
import scalafix.util.logger

import java.io.File

class ScalafixNscPlugin(val global: Global) extends Plugin {
  logger.info("HELLO FROM SCALAFIX!!!")
  val name = "scalafix"
  val description = "Refactoring tool."
  var config: ScalafixConfig = ScalafixConfig()
  val components: List[PluginComponent] =
    new ScalafixNscComponent(this, global, () => config) :: Nil

  override def init(options: List[String], error: (String) => Unit): Boolean = {
    logger.elem(options)
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
