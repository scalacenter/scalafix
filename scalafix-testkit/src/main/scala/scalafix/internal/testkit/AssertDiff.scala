package scalafix.internal.testkit

import scalafix.lint.RuleDiagnostic
import scalafix.internal.util.LintSyntax._

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
      reportedLintMessages: List[RuleDiagnostic],
      expectedLintMessages: List[CommentAssertion]): AssertDiff = {

    val data =
      expectedLintMessages
        .map { assert =>
          reportedLintMessages
            .map(message => AssertDelta(assert, message))
            .to[IndexedSeq]
        }
        .to[IndexedSeq]

    if (reportedLintMessages.nonEmpty && expectedLintMessages.nonEmpty) {
      val matrix =
        new Matrix(
          array = data,
          _rows = expectedLintMessages.size - 1,
          _columns = reportedLintMessages.size - 1
        )

      val unreported =
        matrix.rows
          .filter(_.forall(_.isWrong))
          .flatMap(_.headOption.map(_.assert))
          .toList

      val unexpected =
        matrix.columns
          .filter(_.forall(_.isWrong))
          .flatMap(_.headOption.map(_.lintDiagnostic))
          .toList

      val mismatch =
        matrix.cells.filter(_.isMismatch).toList

      AssertDiff(
        unreported = unreported,
        unexpected = unexpected,
        mismatch = mismatch
      )
    } else {
      AssertDiff(
        unreported = expectedLintMessages,
        unexpected = reportedLintMessages,
        mismatch = List()
      )
    }
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
    unexpected: List[RuleDiagnostic],
    mismatch: List[AssertDelta]) {

  def isFailure: Boolean =
    !(
      unreported.isEmpty &&
        unexpected.isEmpty &&
        mismatch.isEmpty
    )

  override def toString: String = {
    val nl = "\n"

    def formatDiagnostic(diag: RuleDiagnostic): String = {
      diag.withMessage("\n" + diag.message).formattedMessage
    }

    val elementSeparator =
      nl + nl + "---------------------------------------" + nl + nl

    val mismatchBanner =
      if (mismatch.isEmpty) ""
      else {
        """|===========> Mismatch  <===========
           |
           |""".stripMargin
      }

    val showMismatchs =
      mismatch
        .sortBy(_.lintDiagnostic.position.startLine)
        .map { delta =>
          List(
            "Obtained: " + formatDiagnostic(delta.lintDiagnostic),
            "Expected: " + delta.assert.formattedMessage,
            "Diff:",
            delta.similarity
          ).mkString("", nl, "")
        }
        .mkString(
          mismatchBanner,
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
        .map(formatDiagnostic)
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
