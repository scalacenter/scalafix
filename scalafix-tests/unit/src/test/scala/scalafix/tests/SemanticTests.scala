package scalafix.tests

import scala.meta._
import scalafix.testkit._

class SemanticTests
    extends SemanticRewriteSuite(
      Database.load(Classpath(AbsolutePath(BuildInfo.mirrorClasspath))),
      AbsolutePath(BuildInfo.inputSourceroot),
      AbsolutePath(BuildInfo.outputSourceroot)
    ) {

//  DiffTest.fromFile(BuildInfo.testsInputResources).groupBy(_.spec).foreach {
//    case (spec, tests) =>
//      println(s"SPEC: $spec")
//      tests.foreach { test =>
//        println()
//        println(test.expected)
//      }
//  }
}
