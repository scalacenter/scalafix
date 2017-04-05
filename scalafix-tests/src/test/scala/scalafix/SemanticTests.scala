package scalafix

class SemanticTests
    extends testkit.SemanticRewriteSuite(
      // classpath to use for compiling .source files.
      // In the scalafix repo, the system property is populated from build.sbt
      sys.props("sbt.paths.scalafixNsc.test.classes")) {
  // directory containing .source files
  val testDir = "scalafix-tests/src/test/resources"
  testkit.DiffTest.testsToRun(new java.io.File(testDir)).foreach { dt =>
    if (dt.skip) {
      ignore(dt.fullName) {}
    } else {
      test(dt.fullName) {
        check(dt.original, dt.expected, dt)
      }
    }
  }
}
