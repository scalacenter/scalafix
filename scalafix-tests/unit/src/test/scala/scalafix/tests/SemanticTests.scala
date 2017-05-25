package scalafix.tests

import scala.meta._
import scalafix.testkit._

class SemanticTests
    extends SemanticRewriteSuite(
      Database.load(Classpath(AbsolutePath(BuildInfo.mirrorClasspath))),
      AbsolutePath(BuildInfo.inputSourceroot),
      AbsolutePath(BuildInfo.outputSourceroot)
    ) {
  runAllTests()
}
