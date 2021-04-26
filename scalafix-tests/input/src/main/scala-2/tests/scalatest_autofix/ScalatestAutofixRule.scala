/*
rule = ScalatestAutofixRule
*/
package tests.scalatest_autofix

import org.scalatest_autofix.Matchers._

object ScalatestAutofixRule {
  def foo(): Unit = shouldBe(1)
}
