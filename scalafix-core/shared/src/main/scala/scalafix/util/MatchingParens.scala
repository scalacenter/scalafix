package scalafix.util

import scala.meta._
import scalafix.internal.util.TokenOps._
import tokens.Token._

sealed abstract class MatchingParens(map: Map[TokenHash, Token]) {
  private def lookup(token: Token) = map.get(hash(token))
  def close(open: Token.LeftParen): Option[Token.RightParen] =
    lookup(open).collect { case x: Token.RightParen => x }
  def close(open: Token.LeftBracket): Option[Token.RightBracket] =
    lookup(open).collect { case x: Token.RightBracket => x }
  def close(open: Token.LeftBrace): Option[Token.RightBrace] =
    lookup(open).collect { case x: Token.RightBrace => x }
  def open(close: Token.RightParen): Option[Token.LeftParen] =
    lookup(close).collect { case x: Token.LeftParen => x }
  def open(close: Token.RightBracket): Option[Token.LeftBracket] =
    lookup(close).collect { case x: Token.LeftBracket => x }
  def open(close: Token.RightBrace): Option[Token.LeftBrace] =
    lookup(close).collect { case x: Token.LeftBrace => x }
}

object MatchingParens {
  private def assertValidParens(open: Token, close: Token): Unit = {
    (open, close) match {
      case (Interpolation.Start(), Interpolation.End()) =>
      case (LeftBrace(), RightBrace()) =>
      case (LeftBracket(), RightBracket()) =>
      case (LeftParen(), RightParen()) =>
      case (o, c) =>
        throw new IllegalArgumentException(s"Mismatching parens ($o, $c)")
    }
  }

  /**
    * Finds matching parens [({})].
    *
    * Contains lookup keys in both directions, opening [({ and closing })].
    */
  private def getMatchingParentheses(tokens: Tokens): Map[TokenHash, Token] = {
    val ret = Map.newBuilder[TokenHash, Token]
    var stack = List.empty[Token]
    tokens.foreach {
      case open @ (LeftBrace() | LeftBracket() | LeftParen() |
          Interpolation.Start()) =>
        stack = open :: stack
      case close @ (RightBrace() | RightBracket() | RightParen() |
          Interpolation.End()) =>
        val open = stack.head
        assertValidParens(open, close)
        ret += hash(open) -> close
        ret += hash(close) -> open
        stack = stack.tail
      case _ =>
    }
    val result = ret.result()
    result
  }
  def apply(tokens: Tokens): MatchingParens =
    new MatchingParens(getMatchingParentheses(tokens)) {}
}
