package scalafix.testkit

import scala.meta._
import scalafix.Rule
import scalafix.syntax._
import scalafix.internal.config.ScalafixConfig
import scalafix.rule.RuleCtx

/** Utility to unit test syntactic rules */
trait BaseSyntacticRuleSuite extends BaseScalafixSuite {

  /**  rule the default rule to use from `check`/`checkDiff`. */
  def rule: Rule
  def check(name: String, original: String, expected: String): Unit = {
    check(rule, name, original, expected)
  }

  def check(
      rule: Rule,
      name: String,
      original: String,
      expected: String): Unit = {
    scalafixTest(name) {
      import scala.meta._
      val obtained = rule.apply(Input.String(original))
      assertNoDiff(obtained, expected)
    }
  }

  def checkDiff(original: Input, expected: String): Unit = {
    checkDiff(rule, original, expected)
  }

  def checkDiff(rule: Rule, original: Input, expected: String): Unit = {
    scalafixTest(original.label) {
      val ctx = RuleCtx(original.parse[Source].get, ScalafixConfig.default)
      val obtained = rule.diff(ctx)
      assert(obtained == expected)
    }
  }
}
