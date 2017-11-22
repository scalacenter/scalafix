package scalafix
package testkit

import org.scalatest.FunSuite

import scala.meta.Position
import scala.meta.inputs.Input
import scala.meta.parsers.Parse
import scala.meta.dialects.Scala212

import scalafix.lint.{LintMessage, LintSeverity}

class AssertDeltaSuite() extends FunSuite {
  test("associate assert and reported message") {
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

    def disable(offset: Int): LintMessage =
      LintMessage(
        message = "Option.get is the root of all evils",
        position = Position.Range(input, offset, offset),
        category = LintCategory(
          id = "Disable.get",
          explanation = "",
          severity = LintSeverity.Error
        )
      )

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

    // println(obtained)

    val expected =
      """|===========> Mismatch  <===========
         |
         |(A lint message was reported on a line with an assert but it
         |does not fully matches.)
         |
         |Obtained: foo/bar/Disable.scala:2: error: [Disable.get]: 
         |Option.get is the root of all evils
         |  Option(1).get /* assert: Disable.get
         |            ^
         |Expected: foo/bar/Disable.scala:2: error: 
         |Option.get is the root of all evils
         |  Option(1).get /* assert: Disable.get
         |             ^
         |Diff:
         |  Option(1).get 
         |             ^-- asserted
         |            ^-- reported
         |
         |
         |---------------------------------------
         |
         |Obtained: foo/bar/Disable.scala:7: error: [Disable.get]: 
         |Option.get is the root of all evils
         |  Option(2).get // assert: Disable.foo
         |            ^
         |Expected: foo/bar/Disable.scala:7: error: 
         |  Option(2).get // assert: Disable.foo
         |                ^
         |Diff:
         |-Disable.foo
         |+Disable.get
         |
         |===========> Unexpected <===========
         |
         |(A lint message was reported but not expected.)
         |
         |foo/bar/Disable.scala:11: error: [Disable.get]: 
         |Option.get is the root of all evils
         |  Option(4).get
         |            ^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:15: error: [Disable.get]: 
         |Option.get is the root of all evils
         |  Option(6).get
         |            ^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:19: error: [Disable.get]: 
         |Option.get is the root of all evils
         |  Option(8).get
         |            ^
         |
         |===========> Unreported <===========
         |
         |(There is an assert but no lint message was reported)
         |
         |foo/bar/Disable.scala:9: error: 
         |  3 // assert: Disable.get
         |    ^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:13: error: 
         |  5 // assert: Disable.get
         |    ^
         |
         |---------------------------------------
         |
         |foo/bar/Disable.scala:17: error: 
         |  7 // assert: Disable.get
         |    ^
         |""".stripMargin

    assert(expected == obtained)
  }
}
