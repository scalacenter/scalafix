package scalafix.testkit

import org.scalatest.FunSuiteLike

@deprecated(
  "Use AbstractSyntacticRuleSuite with the styling trait of your choice mixed-in (*SpecLike or *SuiteLike)",
  "0.9.18"
)
class SyntacticRuleSuite() extends AbstractSyntacticRuleSuite with FunSuiteLike
