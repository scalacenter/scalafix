package scalafix.testkit

import org.scalatest.FunSuiteLike
import scalafix.v0.Rule

@deprecated(
  "Use AbstractSyntacticRuleSuite with the styling trait of your choice mixed-in (*SpecLike or *SuiteLike)",
  "0.9.18"
)
class SyntacticRuleSuite(rule: Rule = Rule.empty)
    extends AbstractSyntacticRuleSuite(rule)
    with FunSuiteLike
