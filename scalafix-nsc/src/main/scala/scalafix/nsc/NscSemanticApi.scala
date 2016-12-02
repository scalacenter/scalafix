package scalafix.nsc

import scala.collection.mutable
import scala.meta.Dialect
import scala.reflect.internal.util.SourceFile
import scala.{meta => m}
import scalafix.Fixed
import scalafix.Scalafix
import scalafix.ScalafixConfig
import scalafix.rewrite.SemanticApi

trait NscSemanticApi extends ReflectToolkit {

  /** Removes redudant Foo.this.ActualType prefix from a type */
  private def stripRedundantThis(typ: m.Type): m.Type =
    typ.transform {
      case m.Term.Select(m.Term.This(m.Name.Indeterminate(_)), qual) => qual
      case m.Type.Select(m.Term.This(m.Name.Indeterminate(_)), qual) => qual
    }.asInstanceOf[m.Type]

  /** Returns a map from byte offset to type name at that offset. */
  private def offsetToType(gtree: g.Tree,
                           dialect: Dialect): mutable.Map[Int, m.Type] = {
    val builder = mutable.Map.empty[Int, m.Type]
    def add(gtree: g.Tree, enclosingPackage: String): Unit = {
      // TODO(olafur) Come up with more principled approach, this is hacky as hell.
      val typename = gtree.toString().stripPrefix(enclosingPackage)
      val parsed = dialect(typename).parse[m.Type]
      parsed match {
        case m.Parsed.Success(ast) =>
          builder(gtree.pos.point) = stripRedundantThis(ast)
        case _ =>
      }
    }
    def foreach(gtree: g.Tree, enclosingPkg: String): Unit = {
      gtree match {
        case g.ValDef(_, _, tpt, _) if tpt.nonEmpty => add(tpt, enclosingPkg)
        case g.DefDef(_, _, _, _, tpt, _) => add(tpt, enclosingPkg)
        case _ =>
      }
      gtree match {
        case g.PackageDef(pid, _) =>
          gtree.children.foreach(x => foreach(x, pid.symbol.fullName + "."))
        case _ =>
          gtree.children.foreach(x => foreach(x, enclosingPkg))
      }
    }
    foreach(gtree, "")
    builder
  }

  private def getSemanticApi(unit: g.CompilationUnit,
                             config: ScalafixConfig): SemanticApi = {
    val offsets = offsetToType(unit.body, config.dialect)
    new SemanticApi {
      override def typeSignature(defn: m.Defn): Option[m.Type] = {
        defn match {
          case m.Defn.Val(_, Seq(pat), _, _) =>
            offsets.get(pat.pos.start.offset)
          case m.Defn.Def(_, name, _, _, _, _) =>
            offsets.get(name.pos.start.offset)
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
