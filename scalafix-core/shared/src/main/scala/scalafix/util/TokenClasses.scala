package scalafix.util

import scala.meta.classifiers.Classifier
import scala.meta.tokens.Token
import scala.meta.tokens.Token._

trait Whitespace
object Whitespace {
  implicit val WhitespaceClassifier: Classifier[Token, Whitespace] =
    new Classifier[Token, Whitespace] {
      override def apply(token: Token): Boolean =
        token.is[Space] || token.is[Tab] || token.is[Newline] || token.is[FF]
    }
}

trait Trivia
object Trivia {
  implicit val TriviaClassifier: Classifier[Token, Trivia] =
    new Classifier[Token, Trivia] {
      override def apply(token: Token): Boolean =
        token.is[Whitespace] || token.is[Comment]
    }
}

trait Newline
object Newline {
  implicit val NewlineClassifier: Classifier[Token, Newline] =
    new Classifier[Token, Newline] {
      override def apply(token: Token): Boolean =
        token.is[LF] || token.is[CR]
    }
}
