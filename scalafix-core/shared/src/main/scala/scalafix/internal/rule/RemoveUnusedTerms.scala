package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

case class RemoveUnusedTerms(index: SemanticdbIndex)
    extends SemanticRule(index, "RemoveUnusedTerms") {

  private val unusedTerms = {
    val UnusedLocalVal = """local (.*) is never used""".r
    index.messages.toIterator.collect {
      case Message(pos, _, UnusedLocalVal(_*))  =>
        pos
    }.toSet
  }

  private def isUnused(defn: Defn) =
    unusedTerms.contains(defn.pos)

  private def removeLiterals(rhs: Term): String =
    rhs match {
      case Lit(_) => ""
      case r => r.syntax
    }

  override def fix(ctx: RuleCtx): Patch = {
    ctx.debugIndex()
    ctx.tree.collect {
      case i: Defn.Val if isUnused(i) => ctx.replaceTree(i, removeLiterals(i.rhs))
      case i: Defn.Var if isUnused(i) => ctx.replaceTree(i, i.rhs.fold("")(removeLiterals))
    }.asPatch
  }
}
