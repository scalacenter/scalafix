/*
rule = ScalatestAutofixRule
*/
package test.testRules

import org.scalatest_autofix.Matchers._

object ScalatestAutofixRule {
  def foo(): Unit = shouldBe(1)
}
