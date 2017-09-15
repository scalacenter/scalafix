package scalafix.internal.rule

import scala.meta._
import scalafix.{Patch, SemanticdbIndex}
import scalafix.rule.{RuleCtx, SemanticRule}

case class NoUnitInsertion(index: SemanticdbIndex)
    extends SemanticRule(index, "NoUnitInsertion") {

  private[this] def insertUnit(ctx: RuleCtx, t: Term.Apply): Patch =
    ctx.addRight(t.tokens.init.last, "()")

  override def fix(ctx: RuleCtx): Patch = {
    val adaptations = index.messages.toIterator.collect {
      case Message(pos, _, msg)
          if msg.startsWith("Adaptation of argument list by inserting ()") =>
        pos
    }.toSet

    ctx.tree.collect {
      case t: Term.Apply if t.args.isEmpty && adaptations.contains(t.pos) =>
        insertUnit(ctx, t)
    }.asPatch
  }
}
