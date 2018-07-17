package scalafix.internal.util

import scala.meta.inputs.Position
import scala.annotation.tailrec
import scala.meta.tokens.{Token, Tokens}
import scalafix.patch.Patch

object SuppressOps {

  /** Binary searches for the last leading token before `pos` */
  @tailrec
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

  def addComments(tokens: Tokens, positions: List[Position]): Patch =
    positions
      .map(pos =>
        findClosestLeadingToken(tokens, pos) match {
          case Some(token) => Patch.addRight(token, "/* scalafix:ok */")
          case _ => Patch.empty
      })
      .asPatch
}
