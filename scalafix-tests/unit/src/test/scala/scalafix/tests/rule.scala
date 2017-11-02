package scalafix.tests

import scalafix.testkit.utest.SemanticRuleSuite
import scalafix.tests.SemanticTests.index
import org.langmeta.io.AbsolutePath

object rule
    extends SemanticRuleSuite(
      index,
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputSbtSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}
