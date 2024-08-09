package scalafix.tests.config

import scalafix.internal.config.ScalaVersion
import scalafix.internal.config.ScalaVersion._

class ScalaVersionSuite extends munit.FunSuite {

  test("parse: 2") {
    val scala2 = ScalaVersion.from("2")
    assert(scala2.get == ScalaVersion.scala2)
  }

  test("parse: 2.13") {
    val scala2 = ScalaVersion.from("2.13")
    assert(scala2.get == Minor(Major.Scala2, 13))
  }

  test("parse: 2.13.5") {
    val scala2 = ScalaVersion.from("2.13.5")
    assert(scala2.get == Patch(Major.Scala2, 13, 5))
  }

  test("parse: 3.0.0-RC3") {
    val scala2 = ScalaVersion.from("3.0.0-RC3")
    assert(scala2.get == RC(Major.Scala3, 0, 0, 3))
  }

  test("parse failure: 3.0.0RC3") {
    val scala2 = ScalaVersion.from("3.0.0RC3")
    assert(scala2.isFailure)
  }

}
