package scalafix.util

import scala.meta.classifiers.Classifier
import scala.meta.classifiers.XtensionClassifiable
import scala.meta.tokens.Token

trait Whitespace
object Whitespace {
  def unapply(token: Token): Boolean = token.is[Token.Whitespace]
  implicit def classifier[T <: Token]: Classifier[T, Whitespace] =
    new Classifier[T, Whitespace] {
      override def apply(token: T): Boolean = unapply(token)
    }
}

trait Trivia
object Trivia {
  def unapply(token: Token): Boolean = token.is[Token.Trivia]
  implicit def classifier[T <: Token]: Classifier[T, Trivia] =
    new Classifier[T, Trivia] {
      override def apply(token: T): Boolean = unapply(token)
    }
}

trait Newline
object Newline {
  def unapply(token: Token): Boolean = token.is[Token.AtEOL]
  implicit def classifier[T <: Token]: Classifier[T, Newline] =
    new Classifier[T, Newline] {
      override def apply(token: T): Boolean = unapply(token)
    }
}
