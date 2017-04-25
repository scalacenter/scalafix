package scalafix.tests

import scalafix.testkit._

class SemanticTests
    extends SemanticRewriteSuite( /* optionally pass in custom classpath */ ) {
  // directory containing .source files
  val testDir = "scalafix-tests/src/test/resources"
  DiffTest.fromFile(new java.io.File(testDir)).foreach(runDiffTest)
}
