package scalafix.util

import scala.meta.classifiers.Classifier
import scala.meta.tokens.Token
import scala.meta.tokens.Token._

trait Whitespace
object Whitespace {
  def unapply(token: Token): Boolean =
    token.is[Space] || token.is[Tab] || token.is[Newline] || token.is[FF]
  implicit def classifier[T <: Token]: Classifier[T, Whitespace] =
    new Classifier[T, Whitespace] {
      override def apply(token: T): Boolean = unapply(token)
    }
}

trait Trivia
object Trivia {
  def unapply(token: Token): Boolean =
    token.is[Whitespace] || token.is[Comment]
  implicit def classifier[T <: Token]: Classifier[T, Trivia] =
    new Classifier[T, Trivia] {
      override def apply(token: T): Boolean = unapply(token)
    }
}

trait Newline
object Newline {
  def unapply(token: Token): Boolean =
    token.is[LF] || token.is[CR]
  implicit def classifier[T <: Token]: Classifier[T, Newline] =
    new Classifier[T, Newline] {
      override def apply(token: T): Boolean = unapply(token)
    }
}
