package scalafix
package rewrite

import scala.meta._

case class NoAutoTupling(mirror: Mirror) extends SemanticRewrite(mirror) {

  private[this] def addWrappingParens(ctx: RewriteCtx,
                                      args: Seq[Term.Arg]): Patch =
    ctx.addLeft(args.head.tokens.head, "(") +
      ctx.addRight(args.last.tokens.last, ")")

  override def rewrite(ctx: RewriteCtx): Patch = {
    val adaptations = mirror.database.messages.toIterator.collect {
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
