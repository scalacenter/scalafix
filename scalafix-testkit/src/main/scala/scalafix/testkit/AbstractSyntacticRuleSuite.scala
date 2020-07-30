package scalafix.testkit

import scala.meta._

import org.scalatest.Suite
import org.scalatest.Tag
import org.scalatest.TestRegistration
import scalafix.internal.config.ScalafixConfig
import scalafix.syntax._
import scalafix.v0._

/** Utility to unit test syntactic rules.
 * <p>
 * Mix-in FunSuiteLike (ScalaTest 3.0), AnyFunSuiteLike (ScalaTest 3.1+) or
 * the testing style of your choice if you add your own tests.
 *
 * @param rule the default rule to use from `check`/`checkDiff`.
 */
abstract class AbstractSyntacticRuleSuite(rule: Rule = Rule.empty)
    extends Suite
    with TestRegistration
    with DiffAssertions {

  def check(name: String, original: String, expected: String): Unit = {
    check(rule, name, original, expected)
  }

  def check(
      rule: Rule,
      name: String,
      original: String,
      expected: String
  ): Unit = {
    check(rule, name, original, expected, Seq(): _*)
  }

  def check(
      rule: Rule,
      name: String,
      original: String,
      expected: String,
      testTags: Tag*
  ): Unit = {
    registerTest(name, testTags: _*) {
      import scala.meta._
      val obtained = rule.apply(Input.String(original))
      assertNoDiff(obtained, expected)
    }
  }

  def checkDiff(original: Input, expected: String): Unit = {
    checkDiff(rule, original, expected, Seq(): _*)
  }

  def checkDiff(original: Input, expected: String, testTags: Tag*): Unit = {
    checkDiff(rule, original, expected, testTags: _*)
  }

  def checkDiff(rule: Rule, original: Input, expected: String): Unit = {
    checkDiff(rule, original, expected, Seq(): _*)
  }

  def checkDiff(
      rule: Rule,
      original: Input,
      expected: String,
      testTags: Tag*
  ): Unit = {
    registerTest(original.label, testTags: _*) {
      val dialect = ScalafixConfig.default.parser.dialectForFile("Source.scala")
      val ctx = RuleCtx(dialect(original).parse[Source].get)
      val obtained = rule.diff(ctx)
      assertNoDiff(obtained, expected)
    }
  }
}
