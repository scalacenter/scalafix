package scalafix.testkit.scalatest

import scalafix.Rule
import scalafix.testkit.BaseSyntacticRuleSuite

class SyntacticRuleSuite(val rule: Rule = Rule.empty)
    extends BaseSyntacticRuleSuite
    with ScalafixSuite
