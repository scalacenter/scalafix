package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.rule.RuleCtx
import scalafix.rule.SemanticRule

case class NoAutoTupling(index: SemanticdbIndex)
    extends SemanticRule(index, "NoAutoTupling") {

  private[this] def addWrappingParens(ctx: RuleCtx, args: Seq[Term]): Patch =
    ctx.addLeft(args.head.tokens.head, "(") +
      ctx.addRight(args.last.tokens.last, ")")

  private[this] def insertUnit(ctx: RuleCtx, t: Term.Apply): Patch =
    ctx.addRight(t.tokens.init.last, "()")

  lazy val unitAdaptations: Set[Position] =
    index.messages.toIterator.collect {
      case Message(pos, _, msg)
          if msg.startsWith("Adaptation of argument list by inserting ()") =>
        pos
    }.toSet

  lazy val tupleAdaptations: Set[Position] =
    index.messages.toIterator.collect {
      case Message(pos, _, msg)
          if msg.startsWith("Adapting argument list by creating a") =>
        pos
    }.toSet

  override def fix(ctx: RuleCtx): Patch = {
    ctx.tree.collect {
      case t: Term.Apply if tupleAdaptations.contains(t.pos) =>
        addWrappingParens(ctx, t.args)
      case t: Term.Apply if t.args.isEmpty && unitAdaptations.contains(t.pos) =>
        insertUnit(ctx, t)
    }.asPatch
  }
}
