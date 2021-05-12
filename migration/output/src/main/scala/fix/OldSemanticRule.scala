package fix

import org.scalatest.FunSuiteLike
import scalafix.testkit.AbstractSemanticRuleSuite

class OldSemanticRule extends AbstractSemanticRuleSuite with FunSuiteLike {
  runAllTests()
}
