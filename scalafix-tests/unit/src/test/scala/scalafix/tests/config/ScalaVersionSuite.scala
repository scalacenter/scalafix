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

  test("parse: 2.13.16-bin-ce78754") {
    val scala2 = ScalaVersion.from("2.13.16-bin-ce78754")
    assert(scala2.get == Nightly(Major.Scala2, 13, 16, None, None, "ce78754"))
    assert(scala2.get.value == "2.13.16-bin-ce78754")
  }

  test("parse: 3.0.0-RC3") {
    val scala3 = ScalaVersion.from("3.0.0-RC3")
    assert(scala3.get == RC(Major.Scala3, 0, 0, 3))
  }

  test("parse: 3.6.0-RC1-bin-20241008-3408ed7-NIGHTLY") {
    val scala3 = ScalaVersion.from("3.6.0-RC1-bin-20241008-3408ed7-NIGHTLY")
    assert(
      scala3.get == Nightly(
        Major.Scala3,
        6,
        0,
        Some(1),
        Some(java.time.LocalDate.parse("2024-10-08")),
        "3408ed7"
      )
    )
    assert(scala3.get.value == "3.6.0-RC1-bin-20241008-3408ed7-NIGHTLY")
  }

  test("parse failure: 3.0.0RC3") {
    val scala3 = ScalaVersion.from("3.0.0RC3")
    assert(scala3.isFailure)
  }

}
