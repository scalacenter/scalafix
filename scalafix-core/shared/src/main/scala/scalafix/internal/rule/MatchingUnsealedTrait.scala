package scalafix.internal.rule

import scalafix.SemanticdbIndex
import scalafix.internal.util.EagerInMemorySemanticdbIndex
import scalafix.lint.{LintCategory, LintMessage}
import scalafix.rule.{RuleCtx, SemanticRule}

import scala.meta.Term.Match
import scala.meta._
import scala.meta.internal.semanticdb3.ClassInfoType
import scala.meta.internal.semanticdb3.Type.{Tag => t}
import scala.meta.internal.{semanticdb3 => s}

case class MatchingUnsealedTrait(index: SemanticdbIndex)
    extends SemanticRule(index, "MatchingUnsealedTrait") {

  override def description: String =
    "Ensure catch-all when matching on unsealed traits"

  private type Sym = String

  private val errorCategory: LintCategory =
    LintCategory.error("Some constructs are unsafe to use and should be avoided")

  private def error(sym: Sym, tree: Tree): LintMessage =
    errorCategory.copy(id = "mustProvideCatchAll").at("", tree.pos)

  private object NotSealedTrait {
    val internalIdx: EagerInMemorySemanticdbIndex =
      index.asInstanceOf[EagerInMemorySemanticdbIndex]

    def followTypeRef(symbol: Sym): Option[s.Type] =
      internalIdx.info(symbol).flatMap(_.tpe)

    /**
      * @param lastSym `s.ClassInfoType` doesn't bundle it's name, so pass around what we looked up
      * @param tpe current type we follow
      * @return
      */
    def lookupClassType(
        lastSym: Sym,
        tpe: s.Type): Option[(Sym, s.ClassInfoType)] = {
      tpe.tag match {
        case t.TYPE_REF =>
          for {
            tr <- tpe.typeRef
            referenced <- followTypeRef(tr.symbol)
            ret <- lookupClassType(tr.symbol, referenced)
          } yield ret

        case t.METHOD_TYPE =>
          for {
            mt <- tpe.methodType
            rt <- mt.returnType
            ct <- lookupClassType(lastSym, rt)
          } yield ct

        case t.CLASS_INFO_TYPE =>
          Some((lastSym, tpe.classInfoType.get))

        case t.BY_NAME_TYPE =>
          for {
            byName <- tpe.byNameType
            byNameTpe <- byName.tpe
            c <- lookupClassType(lastSym, byNameTpe)
          } yield c

        case t.TYPE_TYPE =>
          for {
            typeType <- tpe.typeType
            lower <- typeType.lowerBound
            c <- lookupClassType(lastSym, lower)
          } yield c

        case other =>
          None
      }
    }

    def name(term: Term): Option[Term.Name] =
      term match {
        case x: Term.Name => Some(x)
        case Term.Apply(x: Term.Name, _) => Some(x)
        case _ => None
      }

    def unapply(term: Term): Option[(Sym, s.ClassInfoType)] = {
      import scalafix.internal.util.Implicits.XtensionSymbolInformationProperties

      for {
        termName <- name(term)
        sym <- internalIdx.symbol(termName)
        tr <- followTypeRef(sym.syntax)
        (sym, c: ClassInfoType) <- lookupClassType(sym.syntax, tr)
        cc <- internalIdx.info(sym)
        if cc.kind == s.SymbolInformation.Kind.TRAIT
        if !cc.is(s.SymbolInformation.Property.SEALED)
      } yield (sym, c)
    }
  }

  private object HasNoCatchAll {
    def has(cases: List[scala.meta.Case]): Boolean = {
      cases.last match {
        case Case(Pat.SeqWildcard(), None, _) => true
        case Case(Pat.Wildcard(), None, _) => true
        case Case(Pat.Var(_), None, _) => true
        case _ => false
      }
    }

    def unapply(cases: List[scala.meta.Case]): Boolean = !has(cases)
  }

  override def check(ctx: RuleCtx): Seq[LintMessage] =
    ctx.tree.collect {
      case m@Match(NotSealedTrait((sym, cls)), HasNoCatchAll()) => error(sym, m)
    }
}
