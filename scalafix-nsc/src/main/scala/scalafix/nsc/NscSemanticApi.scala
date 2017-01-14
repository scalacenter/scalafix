package scalafix.nsc

import scala.collection.mutable
import scala.meta.Dialect
import scala.meta.Type
import scala.reflect.internal.util.SourceFile
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.rewrite.SemanticApi
import scalafix.util.logger

case class SemanticContext(enclosingPackage: String, inScope: List[String])

trait NscSemanticApi extends ReflectToolkit {
  case class ImportOracle(oracle: mutable.Map[Int, g.Scope])

  private class ScopeTraverser(val acc: g.Scope) extends g.Traverser {
    override def traverse(t: g.Tree): Unit = {
      t match {
        case g.Import(expr, selectors) =>
        case _ => super.traverse(t)
      }
    }
  }

  private class OffsetTraverser extends g.Traverser {
    val offsets = mutable.Map[Int, g.Tree]()
    val treeOwners = mutable.Map[g.Tree, g.Tree]()
    override def traverse(t: g.Tree): Unit = {
      t match {
        case g.ValDef(_, _, tpt, _) if tpt.nonEmpty =>
          offsets += (tpt.pos.point -> tpt)
        case _ => super.traverse(t)
      }
    }
  }

  private def getSemanticApi(unit: g.CompilationUnit,
                             config: ScalafixConfig): SemanticApi = {
    if (!g.settings.Yrangepos.value) {
      val instructions = "Please re-compile with the scalac option -Yrangepos enabled"
      val explanation  = "This option is necessary for the semantic API to function"
      sys.error(s"$instructions. $explanation")
    }

    val traverser = new OffsetTraverser
    traverser.traverse(unit.body)

    def toMetaType(tp: g.Tree) =
      config.dialect(tp.toString).parse[m.Type].get
      
    new SemanticApi {
      override def shortenType(tpe: m.Type, pos: m.Position): (m.Type, Seq[m.Ref]) = {
        val gtree = traverser.offsets(pos.start.offset)
        val ownerTree = traverser.treeOwners(gtree)
        (tpe -> Nil)
      }
      override def typeSignature(defn: m.Defn): Option[m.Type] = {
        defn match {
          case m.Defn.Val(_, Seq(pat), _, _) =>
            traverser.offsets.get(pat.pos.start.offset).map(toMetaType)
          case m.Defn.Def(_, name, _, _, _, _) =>
            traverser.offsets.get(name.pos.start.offset).map(toMetaType)
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
    val api = getSemanticApi(unit, config)
    val input = getMetaInput(unit.source)
    Scalafix.fix(input, config, Some(api))
  }
}
