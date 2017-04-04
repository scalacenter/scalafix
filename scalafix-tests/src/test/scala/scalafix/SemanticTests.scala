package scalafix

class SemanticTests extends SemanticRewriteSuite {
  // directory containing .source files
  val testDir = "scalafix-tests/src/test/resources"
  DiffTest.testsToRun(new java.io.File(testDir)).foreach { dt =>
    if (dt.skip) {
      ignore(dt.fullName) {}
    } else {
      test(dt.fullName) {
        check(dt.original, dt.expected, dt)
      }
    }
  }
}
