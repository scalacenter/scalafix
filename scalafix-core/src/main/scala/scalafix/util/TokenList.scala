package scalafix.util

import scalafix.util.Compat._
import scala.meta.tokens.Token
import scala.meta.tokens.Tokens

/** Helper to traverse tokens as a doubly linked list.  */
final class TokenList private (tokens: Tokens) {
  def trailing(token: Token): View[Token] =
    tokens.view.slice(tok2idx(token) + 1, tokens.length)

  def leading(token: Token): View[Token] =
    tokens.view.slice(0, tok2idx(token)).reverse

  private[this] val tok2idx = {
    val map = Map.newBuilder[Token, Int]
    var i = 0
    tokens.foreach { tok =>
      map += (tok -> i)
      i += 1
    }
    map
      .result()
      .withDefault(t => throw new NoSuchElementException(s"token not found: $t")
      )
  }

  def find(start: Token)(p: Token => Boolean): Option[Token] =
    tokens.drop(tok2idx(start)).find(p)

  def slice(from: Token, to: Token): SeqView[Token] =
    tokens.view.slice(tok2idx(from), tok2idx(to))

  /** Returns the next/trailing token or the original token if none exists.
   *
   * @note You need to guard against infinite recursion if iterating through
   *       a list of tokens using this method. This method does not fail
   *       with an exception.
   */
  def next(token: Token): Token = {
    tok2idx.get(token) match {
      case Some(i) if tokens.length > i + 1 =>
        tokens(i + 1)
      case _ => token
    }
  }

  /** Returns the previous/leading token or the original token if none exists.
   *
   * @note You need to guard against infinite recursion if iterating through
   *       a list of tokens using this method. This method does not fail
   *       with an exception.
   */
  def prev(token: Token): Token = {
    tok2idx.get(token) match {
      case Some(i) if i > 0 =>
        tokens(i - 1)
      case _ => token
    }
  }

  def leadingSpaces(token: Token): View[Token] =
    leading(token).takeWhile(_.is[Token.Space])

  def trailingSpaces(token: Token): View[Token] =
    trailing(token).takeWhile(_.is[Token.Space])
}

object TokenList {
  def apply(tokens: Tokens): TokenList = new TokenList(tokens)
}
