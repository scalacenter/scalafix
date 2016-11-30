package scalafix.nsc

import scala.collection.mutable
import scala.{meta => m}
import scala.meta.Defn
import scala.meta.Type
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent
import scala.util.Try
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.rewrite.ExplicitImplicit
import scalafix.rewrite.SemanticApi
import scalafix.util.FileOps

class ScalafixNsc(val global: Global) extends Plugin {
  val name = "scalafix"
  val description = "Fix stuff"
  // The plugin does nothing unless scalafix.enable is set in system properties.
  val components: List[PluginComponent] =
    if (sys.props("scalafix.enable") != null)
      new ScalafixNscComponent(this, global) :: Nil
    else Nil
}

trait ReflectToolkit {
  val global: scala.tools.nsc.Global
  lazy val g: global.type = global
}

trait ScalafixToolkit extends ReflectToolkit

trait NscSemanticApi extends ReflectToolkit {
  private def offsetToType(gtree: g.Tree): mutable.Map[Int, m.Type] = {
    val builder = mutable.Map.empty[Int, m.Type]
    def foreach(gtree: g.Tree): Unit = {
      gtree match {
        case g.ValDef(_, _, tpt, _) if tpt.nonEmpty =>
          import scala.meta.Parsed
          m.dialects.Scala211(tpt.toString()).parse[m.Type] match {
            case Parsed.Success(ast) =>
              builder(tpt.pos.point) = ast
            case _ =>
          }
        case _ =>
      }
      gtree.children.foreach(foreach)
    }
    foreach(gtree)
    builder
  }

  private def getSemanticApi(unit: g.CompilationUnit): SemanticApi = {
    val offsets = offsetToType(unit.body)
    new SemanticApi {
      override def typeSignature(defn: m.Defn): Option[m.Type] = {
        import scala.meta._
        defn match {
          case Defn.Val(_, Seq(pat), _, _) =>
            offsets.get(pat.pos.start.offset)
          case _ =>
            None
        }
      }
    }
  }

  private def getMetaInput(source: SourceFile): m.Input = {
    if (source.file.file != null && source.file.file.isFile)
      m.Input.File(source.file.file)
    else m.Input.String(new String(source.content))
  }

  def fix(unit: g.CompilationUnit, config: ScalafixConfig): Fixed = {
    val api = getSemanticApi(unit)
    val input = getMetaInput(unit.source)
    Scalafix.fix(input, config, Some(api))
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
        if (unit.source.file.exists && unit.source.file.file.isFile) {
          fix(unit, ScalafixConfig(rewrites = List(ExplicitImplicit))) match {
            case Fixed.Success(fixed) =>
              FileOps.writeFile(unit.source.file.file, fixed)
            case Fixed.Failed(e) =>
              g.reporter.warning(unit.body.pos,
                                 "Failed to run scalafix. " + e.getMessage)
          }
        }
      }
    }
  }
}
