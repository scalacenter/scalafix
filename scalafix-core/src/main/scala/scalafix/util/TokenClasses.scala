package scalafix.util

import scala.meta.internal.classifiers.classifier
import scala.meta.tokens.Token
import scala.meta.tokens.Token._

@classifier
trait Whitespace
object Whitespace {
  def unapply(token: Token): Boolean = {
    token.is[Space] || token.is[Tab] || token.is[CR] || token.is[LF] ||
    token.is[FF]
  }
}

@classifier
trait Trivia
object Trivia {
  def unapply(token: Token): Boolean = {
    token.is[Whitespace] || token.is[Comment]
  }
}
