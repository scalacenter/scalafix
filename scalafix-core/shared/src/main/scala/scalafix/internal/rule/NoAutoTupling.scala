package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.SemanticCtx
import scalafix.rule.RewriteCtx
import scalafix.rule.SemanticRule

case class NoAutoTupling(sctx: SemanticCtx) extends SemanticRule(sctx) {

  def name = "NoAutoTupling"

  private[this] def addWrappingParens(ctx: RewriteCtx, args: Seq[Term]): Patch =
    ctx.addLeft(args.head.tokens.head, "(") +
      ctx.addRight(args.last.tokens.last, ")")

  override def fix(ctx: RewriteCtx): Patch = {
    val adaptations = sctx.messages.toIterator.collect {
      case Message(pos, _, msg)
          if msg.startsWith("Adapting argument list by creating a") =>
        pos
    }.toSet

    ctx.tree.collect {
      case t: Term.Apply if adaptations.contains(t.pos) =>
        addWrappingParens(ctx, t.args)
    }.asPatch
  }
}
