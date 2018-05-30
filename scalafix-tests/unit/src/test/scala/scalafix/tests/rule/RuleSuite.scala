package scalafix.tests.rule

import scalafix.testkit._
import scalafix.tests.BuildInfo

class RuleSuite
    extends SemanticRuleSuite(
      BuildInfo.semanticClasspath,
      BuildInfo.inputSourceroot,
      Seq(
        BuildInfo.outputSourceroot,
        BuildInfo.outputDottySourceroot
      )
    ) {
  runAllTests()
}
