package scalafix.util

import org.langmeta.inputs.Position

import scala.annotation.tailrec
import scala.meta.tokens.Token
import scalafix.Patch
import scalafix.rule.RuleCtx

object SuppressOps {
  @tailrec
  private def findTokenWithPos(
      tokens: IndexedSeq[Token],
      pos: Position): Option[Token] = {
    if (tokens.isEmpty) {
      None
    } else {
      val m = tokens.length / 2
      if (tokens(m).pos.end <= pos.start) {
        findTokenWithPos(tokens.drop(m + 1), pos)
      } else if (tokens(m).pos.start > pos.start) {
        findTokenWithPos(tokens.take(m), pos)
      } else {
        Some(tokens(m))
      }
    }
  }

  def addComments(ctx: RuleCtx, positions: List[Position]): Patch =
    positions
      .map(pos =>
        findTokenWithPos(ctx.tokens, pos) match {
          case Some(token) => ctx.addRight(token, "/* scalafix:ok */")
          case _ => Patch.empty
      })
      .asPatch
}
