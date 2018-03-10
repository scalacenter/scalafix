package scalafix.internal.util

import org.langmeta.inputs.Position

import scala.annotation.tailrec
import scala.meta.tokens.{Token, Tokens}
import scalafix.{Patch, Rule}
import scalafix.rule.RuleCtx
import scalafix.util.SemanticdbIndex

object SuppressOps {
  @tailrec
  /** Binary searches for the last leading token before `pos` */
  private def findClosestLeadingToken(
      tokens: Tokens,
      pos: Position): Option[Token] = {
    if (tokens.isEmpty) {
      None
    } else {
      val m = tokens.length / 2
      if (tokens(m).pos.end <= pos.start) {
        findClosestLeadingToken(tokens.drop(m + 1), pos)
      } else if (tokens(m).pos.start > pos.start) {
        findClosestLeadingToken(tokens.take(m), pos)
      } else {
        Some(tokens(m))
      }
    }
  }

  final def applyAndSuppress(rule: Rule, ctx: RuleCtx): String = {
    val (patch, lintMessages) = ctx.filter(
      rule.fixWithName(ctx),
      rule.semanticOption.getOrElse(SemanticdbIndex.empty)
    )
    val suppressPatch =
      addComments(ctx, lintMessages.map(_.position))
    val (fixed, _) =
      Patch(Map(rule.name -> (patch + suppressPatch)), ctx, rule.semanticOption)
    fixed
  }

  def addComments(ctx: RuleCtx, positions: List[Position]): Patch =
    positions
      .map(pos =>
        findClosestLeadingToken(ctx.tokens, pos) match {
          case Some(token) => ctx.addRight(token, "/* scalafix:ok */")
          case _ => Patch.empty
      })
      .asPatch
}
