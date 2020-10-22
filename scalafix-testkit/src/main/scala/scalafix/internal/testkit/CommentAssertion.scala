package scalafix.internal.testkit

import scala.util.matching.Regex

import scala.meta._

import scalafix.internal.util.PositionSyntax._

// CommentAssertion are the bread and butter of testkit they
// assert the line position and the category id of the lint message.
//
// For example:
//
// ```scala
// class Foo {
//   def bar(x: Int = 42) = 1 // assert: DisableSyntax.defaultArgs
// }
// ```
//
// You can also use the multiline variant. This isuseful two visually
// show the lint message it adds an assertion on the message body and
// the caret position of the lint message.
//
// For example:
//
// ```scala
// class Foo {
//   def bar(x: Int = 42) = 1 /* assert: DisableSyntax.defaultArgs
//                    ^^
// Default args makes it hard to use methods as functions.
// */
// }
// ```
// */
case class CommentAssertion(
    anchorPosition: Position,
    key: String,
    caretPosition: Option[Position],
    expectedMessage: Option[String]
) {

  def formattedMessage: String =
    caretPosition
      .getOrElse(anchorPosition)
      .formatMessage(
        "error",
        expectedMessage.map("\n" + _).getOrElse("")
      )
}

object CommentAssertion {
  def extract(tokens: Tokens): List[CommentAssertion] = {
    tokens.collect {
      case EndOfLineAssertExtractor(singleline) =>
        singleline
      case MultiLineAssertExtractor(multiline) =>
        multiline
    }.toList
  }
}

object EndOfLineAssertExtractor {
  val AssertRegex: Regex = " assert: (.*)".r
  def unapply(token: Token.Comment): Option[CommentAssertion] = {
    token match {
      case Token.Comment(AssertRegex(key)) =>
        Some(
          CommentAssertion(
            anchorPosition = token.pos,
            key = key,
            caretPosition = None,
            expectedMessage = None
          )
        )
      case _ =>
        None
    }
  }
}

object MultiLineAssertExtractor {
  private val assertMessage = " assert:"

  def unapply(token: Token.Comment): Option[CommentAssertion] = {
    token match {
      case Token.Comment(content)
          if content.startsWith(assertMessage) &&
            content.contains("\n") => {

        val lines = content.split('\n')

        val key =
          lines(0) match {
            case EndOfLineAssertExtractor.AssertRegex(key) => key
            case _ =>
              throw new Exception(
                "the rule name should be on the first line of the assert"
              )
          }

        val caretOffsetStart = lines(1).indexOf('^')
        if (caretOffsetStart == -1)
          throw new Exception("^ should be on the second line of the assert")
        val caretOffsetEnd = lines(1).lastIndexOf('^')
        val offsetStart = caretOffsetStart - token.pos.startColumn
        val offsetEnd = caretOffsetEnd - token.pos.startColumn + 1
        val caretStart = token.pos.start + offsetStart
        val caretEnd = token.pos.start + offsetEnd
        val message = lines.drop(2).mkString("\n")
        Some(
          CommentAssertion(
            anchorPosition = token.pos,
            key = key,
            caretPosition = Some(
              Position.Range(token.pos.input, caretStart, caretEnd)
            ),
            expectedMessage = Some(message)
          )
        )
      }
      case _ => None
    }
  }
}
