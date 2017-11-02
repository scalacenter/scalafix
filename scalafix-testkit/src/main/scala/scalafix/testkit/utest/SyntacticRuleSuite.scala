package scalafix.testkit.utest

import scalafix.Rule
import scalafix.testkit.BaseSyntacticRuleSuite

class SyntacticRuleSuite(val rule: Rule = Rule.empty)
    extends ScalafixTest
    with BaseSyntacticRuleSuite
