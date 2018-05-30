package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule
import scalafix.v0._

case class RemoveUnusedImports(index: SemanticdbIndex)
    extends SemanticRule(
      index,
      "RemoveUnusedImports"
    ) {

  override def description: String =
    "Rewrite that removes unused imports reported by the compiler under -Xwarn-unused-import."

  private val unusedImports = index.messages.toIterator.collect {
    case Message(pos, _, "Unused import") =>
      pos
  }.toSet
  private def isUnused(importee: Importee) = {
    val pos = importee match {
      case Importee.Rename(name, _) => name.pos
      case _ => importee.pos
    }
    unusedImports.contains(pos)
  }
  override def fix(ctx: RuleCtx): Patch =
    ctx.tree.collect {
      case i: Importee if isUnused(i) => ctx.removeImportee(i).atomic
    }.asPatch
}
