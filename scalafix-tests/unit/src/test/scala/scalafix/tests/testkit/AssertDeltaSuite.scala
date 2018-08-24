package scalafix.tests.testkit

import org.scalatest.FunSuite
import scala.meta.Position
import scala.meta.dialects.Scala212
import scala.meta.inputs.Input
import scala.meta.parsers.Parse
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.testkit.AssertDiff
import scalafix.internal.testkit.CommentAssertion
import scalafix.internal.tests.utils.SkipWindows
import scalafix.v0.LintCategory
import scalafix.lint.RuleDiagnostic
import scalafix.lint.LintSeverity
import scalafix.rule.RuleName
import scalafix.testkit.DiffAssertions
import scalafix.internal.util.LintSyntax._
import scalafix.v0.LintMessage

class AssertDeltaSuite() extends FunSuite with DiffAssertions {
  test("associate assert and reported message", SkipWindows) {
    val input = Input.VirtualFile(
      path = "foo/bar/Disable.scala",
      value = """|case object Disable {
                 |  Option(1).get /* assert: Disable.get
                 |             ^
                 |Option.get is the root of all evils
                 |*/
                 |
                 |  Option(2).get // assert: Disable.foo
                 |
                 |  3 // assert: Disable.get
                 |
                 |  Option(4).get
                 |
                 |  5 // assert: Disable.get
                 |
                 |  Option(6).get
                 |
                 |  7 // assert: Disable.get
                 |
                 |  Option(8).get
                 |}""".stripMargin
    )

    def disable(offset: Int): RuleDiagnostic =
      LintMessage(
        message = "Option.get is the root of all evils",
        position = Position.Range(input, offset, offset),
        category = LintCategory(
          id = "get",
          explanation = "",
          severity = LintSeverity.Error
        )
      ).toDiagnostic(RuleName("Disable"), ScalafixConfig.default)

    // start offset of all `.get`
    val reportedLintMessages = List(
      disable(34),
      disable(128),
      disable(196),
      disable(241),
      disable(286)
    )

    val tokens = Parse.parseSource(input, Scala212).get.tokens

    val expectedLintMessages = CommentAssertion.extract(tokens)

    val diff = AssertDiff(reportedLintMessages, expectedLintMessages)

    assert(diff.isFailure)

    val obtained = diff.toString

    val expected =
      """|===========> Mismatch  <===========
         |
         |Obtained: foo/bar/Disable.scala:2:13: error: [Disable.get]:
         |Option.get is the root of all evils
         |  Option(1).get /* assert: Disable.get
         |            ^
         |Expected: foo/bar/Disable.scala:2:14: error:
         |Option.get is the root of all evils
         |  Option(1).get /* assert: Disable.get
         |             ^
         |Diff:
         |  Option(1).get /* assert: Disable.get
         |             ^-- asserted
         |            ^-- reported
         |
         |
         |---------------------------------------
         |
         |Obtained: foo/bar/Disable.scala:7:13: error: [Disable.get]:
         |Option.get is the root of all evils
         |  Option(2).get // assert: Disable.foo
         |            ^
         |Expected: foo/bar/Disable.scala:7:17: error
         |  Option(2).get // assert: Disable.foo
         |                ^^^^^^^^^^^^^^^^^^^^^^
         |Diff:
         |-Disable.foo
         |+Disable.get
         |
         |===========> Unexpected <===========
         |
         |foo/bar/Disable.scala:11:13: error: [Disable.get]:
         |Option.get is the root of all evils
         |  Option(4).get
         |            ^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:15:13: error: [Disable.get]:
         |Option.get is the root of all evils
         |  Option(6).get
         |            ^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:19:13: error: [Disable.get]:
         |Option.get is the root of all evils
         |  Option(8).get
         |            ^
         |
         |===========> Unreported <===========
         |
         |foo/bar/Disable.scala:9:5: error
         |  3 // assert: Disable.get
         |    ^^^^^^^^^^^^^^^^^^^^^^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:13:5: error
         |  5 // assert: Disable.get
         |    ^^^^^^^^^^^^^^^^^^^^^^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:17:5: error
         |  7 // assert: Disable.get
         |    ^^^^^^^^^^^^^^^^^^^^^^
         |""".stripMargin

    assertNoDiff(expected, obtained)
  }
}
