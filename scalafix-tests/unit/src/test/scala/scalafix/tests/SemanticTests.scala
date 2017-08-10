package scalafix
package tests

import scala.meta._
import scalafix.testkit._

class SemanticTests
    extends SemanticRewriteSuite(
      Mirror.load(Classpath(AbsolutePath(BuildInfo.mirrorClasspath))),
      AbsolutePath(BuildInfo.inputSourceroot),
      Seq(
        AbsolutePath(BuildInfo.outputSourceroot),
        AbsolutePath(BuildInfo.outputDottySourceroot)
      )
    ) {
  runAllTests()
}
