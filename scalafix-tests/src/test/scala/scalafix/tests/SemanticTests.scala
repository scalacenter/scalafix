package scalafix.tests

import scalafix.testkit._

class SemanticTests
    extends SemanticRewriteSuite(
      // Classpath to compile .source files. In the scalafix repo, the classpath
      // is passed in from build.sbt as a system property.
      sys.props("sbt.paths.scalafixNsc.test.classes")) {
  // directory containing .source files
  val testDir = "scalafix-tests/src/test/resources"
  DiffTest.fromFile(new java.io.File(testDir)).foreach(runDiffTest)
}
