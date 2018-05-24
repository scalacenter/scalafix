package scalafix
package testkit

import scala.meta._
import scala.util.matching.Regex
import scala.meta.internal.ScalafixLangmetaHacks

// CommentAssertion are the bread and butter of testkit they
// assert the line position and the category id of the lint message.
//
// For example:
//
// ```scala
// Option(1).get // assert: Disable.get
// ```
//
// You can also use the multiline variant. This isuseful two visually
// show the lint message it adds an assertion on the message body and
// the caret position of the lint message.
//
// For example:
//
// ```scala
// Option(1).get /* assert: Disable.get
//           ^
// Option.get is the root of all evilz
//
// If you must Option.get, wrap the code block with
// // scalafix:off Option.get
// ...
// // scalafix:on Option.get
// */
case class CommentAssertion(
    anchorPosition: Position,
    key: String,
    caretPosition: Option[Position],
    expectedMessage: Option[String]) {

  def formattedMessage: String =
    ScalafixLangmetaHacks.formatMessage(
      caretPosition.getOrElse(anchorPosition),
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
          ))
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
            content.isMultiline => {

        val lines = content.split('\n')

        val key =
          lines(0) match {
            case EndOfLineAssertExtractor.AssertRegex(key) => key
            case _ =>
              throw new Exception(
                "the rule name should be on the first line of the assert")
          }

        val caretOffset = lines(1).indexOf('^')
        if (caretOffset == -1)
          throw new Exception("^ should be on the second line of the assert")
        val offset = caretOffset - token.pos.startColumn
        val caretStart = token.pos.start + offset
        val message = lines.drop(2).mkString("\n")

        Some(
          CommentAssertion(
            anchorPosition = token.pos,
            key = key,
            caretPosition = Some(
              Position.Range(token.pos.input, caretStart, caretStart)
            ),
            expectedMessage = Some(message)
          ))
      }
      case _ => None
    }
  }
}

// AssertDelta is used to find which Assert is associated with which LintMessage
case class AssertDelta(assert: CommentAssertion, lintMessage: LintMessage) {

  private def sameLine(assertPos: Position): Boolean =
    assertPos.startLine == lintMessage.position.startLine

  private def lintKey: String =
    lintMessage.category.id

  private def sameKey(key: String): Boolean =
    lintKey == key

  private val isSameLine: Boolean =
    sameLine(assert.anchorPosition)

  private val isCorrect: Boolean =
    sameKey(assert.key) &&
      (assert.caretPosition match {
        case Some(carPos) =>
          (carPos.start == lintMessage.position.start) &&
            (assert.expectedMessage
              .map(_.trim == lintMessage.message.trim)
              .getOrElse(true))
        case None =>
          sameLine(assert.anchorPosition)
      })

  def isMismatch: Boolean = isSameLine && !isCorrect

  def isWrong: Boolean = !isSameLine

  def similarity: String = {
    val pos = assert.anchorPosition

    val caretDiff =
      assert.caretPosition
        .map { carPos =>
          if (carPos.start != lintMessage.position.start) {
            val line =
              Position
                .Range(pos.input, pos.start - pos.startColumn, pos.start)
                .text

            val assertCarret = (" " * carPos.startColumn) + "^-- asserted"
            val lintCarret = (" " * lintMessage.position.startColumn) + "^-- reported"

            List(
              line,
              assertCarret,
              lintCarret
            )
          } else {
            Nil
          }
        }
        .getOrElse(Nil)

    val keyDiff =
      if (!sameKey(assert.key)) {
        List(
          s"""|-${assert.key}
              |+$lintKey""".stripMargin
        )
      } else {
        Nil
      }

    val messageDiff =
      assert.expectedMessage
        .map(
          message =>
            List(
              DiffAssertions.compareContents(
                message,
                lintMessage.message
              )
          ))
        .getOrElse(Nil)

    val result =
      caretDiff ++
        keyDiff ++
        messageDiff

    result.mkString("\n")
  }
}

// AssertDiff is the result of comparing CommentAssertion and LintMessage
//
// There is three categories of result:
//
//  * unreported: the developper added an assert but no linting was reported
//  * unexpected: a linting was reported but the developper did not asserted it
//  * missmatch: the developper added an assert and a linting wal reported but they partialy match
//            for example, the caret on a multiline assert may be on the wrong offset.
case class AssertDiff(
    unreported: List[CommentAssertion],
    unexpected: List[LintMessage],
    missmatch: List[AssertDelta]) {

  def isFailure: Boolean =
    !(
      unreported.isEmpty &&
        unexpected.isEmpty &&
        missmatch.isEmpty
    )

  override def toString: String = {
    val nl = "\n"

    def formatLintMessage(lintMessage: LintMessage): String = {
      ScalafixLangmetaHacks.formatMessage(
        lintMessage.position,
        s"error: [${lintMessage.category.id}]",
        nl + lintMessage.message
      )
    }

    val elementSeparator =
      nl + nl + "---------------------------------------" + nl + nl

    val missmatchBanner =
      if (missmatch.isEmpty) ""
      else {
        """|===========> Mismatch  <===========
           |
           |""".stripMargin
      }

    val showMismatchs =
      missmatch
        .sortBy(_.lintMessage.position.startLine)
        .map(
          delta =>
            List(
              "Obtained: " + formatLintMessage(delta.lintMessage),
              "Expected: " + delta.assert.formattedMessage,
              "Diff:",
              delta.similarity
            ).mkString("", nl, ""))
        .mkString(
          missmatchBanner,
          elementSeparator,
          nl
        )

    val unexpectedBanner =
      if (unexpected.isEmpty) ""
      else {
        """|===========> Unexpected <===========
           |
           |""".stripMargin
      }

    val showUnexpected =
      unexpected
        .sortBy(_.position.startLine)
        .map(formatLintMessage)
        .mkString(
          unexpectedBanner,
          elementSeparator,
          nl
        )

    val unreportedBanner =
      if (unreported.isEmpty) ""
      else {
        """|===========> Unreported <===========
           |
           |""".stripMargin
      }

    val showUnreported =
      unreported
        .sortBy(_.anchorPosition.startLine)
        .map(_.formattedMessage)
        .mkString(
          unreportedBanner,
          elementSeparator,
          nl
        )

    List(
      showMismatchs,
      showUnexpected,
      showUnreported
    ).mkString(nl)
  }
}

// Example:
//
// ```scala
// //   (R1)                 (A1)
// Option(1).get /* assert: Disable.get
//     ^ (*: caret on wrong offset)
//
// Option.get is the root of all evils
//
// If you must Option.get, wrap the code block with
// // scalafix:off Option.get
// ...
// // scalafix:on Option.get
// */
//
// //    (A2)
// 1 // assert: Disable.get
//
// //   (R2)
// Option(1).get
// ```
//                             Reported
//                                        ------
//                      R1                | R2 |
//                                        |    |
//            A1        ✓*                | ✗  |
//                                        |    |
// Asserted                               |    |<-- (∀ wrong = unexpected)
//                                        |    |
//           -----------------------------+----+
//           |A2        ✗                 | ✗  |
//           -----------------------------+----+
//                ^
//                +-- (∀ wrong = unreported)
object AssertDiff {
  def apply(
      reportedLintMessages: List[LintMessage],
      expectedLintMessages: List[CommentAssertion]): AssertDiff = {

    val data =
      expectedLintMessages
        .map(
          assert =>
            reportedLintMessages
              .map(message => AssertDelta(assert, message))
              .to[IndexedSeq])
        .to[IndexedSeq]

    if (reportedLintMessages.nonEmpty && expectedLintMessages.nonEmpty) {
      val matrix =
        new Matrix(
          array = data,
          rows = expectedLintMessages.size - 1,
          columns = reportedLintMessages.size - 1
        )

      val unreported =
        matrix.rows
          .filter(_.forall(_.isWrong))
          .flatMap(_.headOption.map(_.assert))
          .toList

      val unexpected =
        matrix.columns
          .filter(_.forall(_.isWrong))
          .flatMap(_.headOption.map(_.lintMessage))
          .toList

      val missmatch =
        matrix.cells.filter(_.isMismatch).toList

      AssertDiff(
        unreported = unreported,
        unexpected = unexpected,
        missmatch = missmatch
      )
    } else {
      AssertDiff(
        unreported = expectedLintMessages,
        unexpected = reportedLintMessages,
        missmatch = List()
      )
    }
  }
}

class Matrix[T](array: IndexedSeq[IndexedSeq[T]], rows: Int, columns: Int) {
  def row(r: Int): IndexedSeq[T] = array(r)
  def column(c: Int): IndexedSeq[T] = (0 to rows).map(i => array(i)(c))
  def rows: IndexedSeq[IndexedSeq[T]] = array
  def columns: IndexedSeq[IndexedSeq[T]] = (0 to columns).map(column)
  def cells: IndexedSeq[T] = array.flatten
}
