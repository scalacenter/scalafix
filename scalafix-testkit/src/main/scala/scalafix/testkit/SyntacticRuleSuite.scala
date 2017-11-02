package scalafix
package testkit

import scala.meta._

@deprecated("Moved to scalafix.testkit.scalatest.SyntacticRuleSuite", "0.5.4")
class SyntacticRuleSuite(val rule: Rule = Rule.empty)
    extends scalatest.ScalafixSuite
    with BaseSyntacticRuleSuite
