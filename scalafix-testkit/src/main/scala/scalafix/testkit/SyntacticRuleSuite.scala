package scalafix
package testkit

import scala.meta._
import scalafix.internal.config.ScalafixConfig
import scalafix.rule.RuleCtx
import scalafix.syntax._

import org.scalatest.FunSuiteLike

class SyntacticRuleSuite(defaultRule: Rule = Rule.empty)
    extends FunSuiteLike
    with DiffAssertions {
  def check(name: String, original: String, expected: String): Unit = {
    check(defaultRule, name, original, expected)
  }

  def check(
      rule: Rule,
      name: String,
      original: String,
      expected: String): Unit = {
    test(name) {
      import scala.meta._
      val obtained = rule.apply(Input.String(original))
      assertNoDiff(obtained, expected)
    }
  }

  def checkDiff(original: Input, expected: String): Unit = {
    checkDiff(defaultRule, original, expected)
  }

  def checkDiff(rule: Rule, original: Input, expected: String): Unit = {
    test(original.label) {
      val ctx = RuleCtx(original.parse[Source].get, ScalafixConfig.default)
      val obtained = rule.diff(ctx)
      assert(obtained == expected)
    }
  }
}
