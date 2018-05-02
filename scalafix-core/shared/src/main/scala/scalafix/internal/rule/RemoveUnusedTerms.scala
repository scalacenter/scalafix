package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

case class RemoveUnusedTerms(index: SemanticdbIndex)
    extends SemanticRule(index, "RemoveUnusedTerms") {

  override def description: String =
    "Rewrite that removes unused locals or privates by -Ywarn-unused:locals,privates"

  private val unusedTerms = {
    val UnusedLocalVal = """local (.*) is never used""".r
    index.messages.toIterator.collect {
      case Message(pos, _, UnusedLocalVal(_*)) =>
        pos
    }.toSet
  }

  private val unusedPatternTerms = {
    val UnusedPatternVal = """pattern (.*) is never used(.*)@(.*)""".r
    index.messages.toIterator.collect {
      case Message(pos, _, UnusedPatternVal(_*)) =>
        pos
    }.toSet
  }

  private def isUnusedTerm(defn: Defn) =
    unusedTerms.contains(defn.pos)

  private def isUnusedPatternTerm(defn: Pat.Var) =
    unusedPatternTerms.contains(defn.pos)

  private def removeDeclarationTokens(i: Defn, rhs: Term): Tokens = {
    val startDef = i.tokens.start
    val startBody = rhs.tokens.start
    i.tokens.take(startBody - startDef)
  }

  private def tokensToRemove(defn: Defn): Option[Tokens] = defn match {
    case i @ Defn.Val(_, _, _, Lit(_)) => Some(i.tokens)
    case i @ Defn.Val(_, _, _, rhs) => Some(removeDeclarationTokens(i, rhs))
    case i @ Defn.Var(_, _, _, Some(Lit(_))) => Some(i.tokens)
    case i @ Defn.Var(_, _, _, rhs) => rhs.map(removeDeclarationTokens(i, _))
    case _ => None
  }

  private def addBinding(term: Pat.Var): Option[Tokens] = term match {
    case _ => None
  }

  override def fix(ctx: RuleCtx): Patch =
    ctx.tree.collect {
      case i: Pat.Var if isUnusedPatternTerm(i) =>
        ctx.addRight(i, " @ _").atomic
      case i: Defn if isUnusedTerm(i) =>
        tokensToRemove(i)
          .fold(Patch.empty)(token => ctx.removeTokens(token))
          .atomic
    }.asPatch
}
