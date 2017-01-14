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
    def addAllMembers(sym: g.Symbol) =
      sym.info.members
        .filterNot(s => s.isRoot || s.isPackageObjectOrClass)
        .foreach(acc.enter)
    override def traverse(t: g.Tree): Unit = {
      t match {
        case pkg: g.PackageDef =>
          addAllMembers(pkg.pid.symbol)
        case g.Import(expr, selectors) =>
          logger.elem(t)
          val exprSym = expr.symbol
          exprSym.info.members
            .filter { m =>
              selectors.exists(_.name == m.name)
            }
            .foreach(acc.enter)
          selectors.foreach {
            case isel @ g.ImportSelector(g.TermName("_"), _, null, _) =>
              addAllMembers(exprSym)
          }
        case _ => super.traverse(t)
      }
    }
  }

  private class OffsetTraverser extends g.Traverser {
    val offsets = mutable.Map[Int, g.Tree]()
    val treeOwners = mutable.Map[Int, g.Tree]()
    override def traverse(t: g.Tree): Unit = {
      t match {
        case g.ValDef(_, name, tpt, _) if tpt.nonEmpty =>
          offsets += (tpt.pos.point -> tpt)
          treeOwners += (t.pos.point -> t)
          logger.elem(t, t.pos.point)
        case g.DefDef(_, _, _, _, tpt, _) =>
          offsets += (tpt.pos.point -> tpt)
          treeOwners += (t.pos.point -> t)
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

    def toMetaType(tp: g.Tree) =
      config.dialect(tp.toString).parse[m.Type].get

    def parseAsType(tp: String) =
      config.dialect(tp).parse[m.Type].get

    def gimmePosition(t: m.Tree): m.Position = {
      t match {
        case m.Defn.Val(_, Seq(pat), _, _) => pat.pos
        case m.Defn.Def(_, name, _, _, _, _) => name.pos
        case _ => t.pos
      }
    }

    val st = new ScopeTraverser(g.newScope)
    st.traverse(unit.body)
    logger.elem(st.acc)
    val globallyImported = st.acc.map(s => parseAsType(s.fullName)).toSet

    val traverser = new OffsetTraverser
    traverser.traverse(unit.body)

    new SemanticApi {
      override def shortenType(tpe: m.Type, t: m.Tree): (m.Type, Seq[m.Ref]) = {
        val tpePos = gimmePosition(t).start.offset
        val ownerTree = traverser.treeOwners(tpePos)
        val gtpeTree = traverser.offsets(tpePos)
        val tpeSymbol = gtpeTree.symbol
        val symbol = ownerTree match {
          case st: g.SymTree => st.symbol
          case _ => g.NoSymbol
        }
        logger.elem(globallyImported)

        // TODO(jvican): Only maintain types and packages
        val allMembersOwners = (ownerTree.symbol.ownerChain
            .flatMap(_.info.members.toList)
            .map(_.fullName)) :+ tpeSymbol.fullName

        val missing =
          tpeSymbol.ownerChain
            .takeWhile(s => !st.acc.exists(_.fullName == s.fullName))
            .filterNot { s =>
              allMembersOwners.contains(s.fullName)
            }
            .filterNot(s => s.isEmptyPackage || s.hasPackageFlag)

        val missingRefs =
          missing.map(ms => parseAsType(ms.fullName).asInstanceOf[m.Ref])

        val allImported =
          (globallyImported ++ missingRefs.toSet).map(_.syntax)
        val shortenedTpe = tpe.transform {
          case ref: m.Type.Select if allImported.contains(ref.syntax) =>
            ref.name
          case ref: m.Term.Select if allImported.contains(ref.syntax) =>
            ref.name
          case m.Term.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
            qual
          case m.Type.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
            qual
        }

        logger.elem(shortenedTpe, missingRefs, allImported)
        (shortenedTpe.asInstanceOf[m.Type] -> missingRefs)
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
