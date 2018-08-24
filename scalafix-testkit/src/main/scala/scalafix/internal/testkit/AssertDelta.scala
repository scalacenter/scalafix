package scalafix.internal.testkit

import scala.meta.Position
import scalafix.lint.RuleDiagnostic
import scalafix.testkit.DiffAssertions
import scalafix.internal.util.PositionSyntax._

// AssertDelta is used to find which Assert is associated with which LintMessage
case class AssertDelta(
    assert: CommentAssertion,
    lintDiagnostic: RuleDiagnostic) {

  private def sameLine(assertPos: Position): Boolean =
    assertPos.startLine == lintDiagnostic.position.startLine

  private def sameKey(key: String): Boolean =
    lintDiagnostic.id.fullID == key

  private val isSameLine: Boolean =
    sameLine(assert.anchorPosition)

  private val isCorrect: Boolean =
    sameKey(assert.key) &&
      (assert.caretPosition match {
        case Some(carPos) =>
          (carPos.start == lintDiagnostic.position.start) &&
            assert.expectedMessage.forall(_.trim == lintDiagnostic.message.trim)
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
          if (carPos.start != lintDiagnostic.position.start) {
            val line = pos.lineContent

            val assertCarret = (" " * carPos.startColumn) + "^-- asserted"
            val lintCarret = (" " * lintDiagnostic.position.startColumn) + "^-- reported"

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
              |+${lintDiagnostic.id.fullID}""".stripMargin
        )
      } else {
        Nil
      }

    val messageDiff =
      assert.expectedMessage
        .map { message =>
          List(
            DiffAssertions.compareContents(
              message,
              lintDiagnostic.message
            )
          )
        }
        .getOrElse(Nil)

    val result =
      caretDiff ++
        keyDiff ++
        messageDiff

    result.mkString("\n")
  }
}
