package scalafix.tests.versions

import org.scalatest.funsuite.AnyFunSuite
import scalafix.Versions
import scalafix.interfaces.ScalafixVersions

class ScalafixVersionsImplSuite extends AnyFunSuite {

  lazy val versions: ScalafixVersions = ScalafixVersions.get()

  test("scalafixVersion") {
    assert(versions.scalafixVersion() == Versions.version)
  }

  test("fail to get 2.11 with full version") {
    assertThrows[IllegalArgumentException](
      versions.cliScalaVersion("2.11.0")
    )
  }

  test("fail to get 2.11 with minor version") {
    assertThrows[IllegalArgumentException](
      versions.cliScalaVersion("2.11")
    )
  }

  test("get 2.12 with full version") {
    assert(versions.cliScalaVersion("2.12.20") == Versions.scala212)
  }

  test("get 2.12 with major.minor version") {
    assert(versions.cliScalaVersion("2.12") == Versions.scala212)
  }

  test("get 2.13 with full version") {
    assert(versions.cliScalaVersion("2.13.15") == Versions.scala213)
  }

  test("get 2.13 with major.minor version") {
    assert(versions.cliScalaVersion("2.13") == Versions.scala213)
  }

  test("get 2.13 with major version") {
    assert(versions.cliScalaVersion("2") == Versions.scala213)
  }

  test("get 3LTS with full pre-LTS version") {
    assert(versions.cliScalaVersion("3.0.0") == Versions.scala3LTS)
  }

  test("get 3LTS with major.minor pre-LTS version") {
    assert(versions.cliScalaVersion("3.2") == Versions.scala3LTS)
  }

  test("get 3LTS with full LTS version") {
    assert(versions.cliScalaVersion("3.3.4") == Versions.scala3LTS)
  }

  test("get 3LTS with major.minor LTS version") {
    assert(versions.cliScalaVersion("3.3") == Versions.scala3LTS)
  }

  test("get 3.5 with full version") {
    assert(versions.cliScalaVersion("3.5.2") == Versions.scala35)
  }

  test("get 3.5 with major.minor version") {
    assert(versions.cliScalaVersion("3.5") == Versions.scala35)
  }

  test("get 3.6 with full version") {
    assert(versions.cliScalaVersion("3.6.3") == Versions.scala36)
  }

  test("get 3.6 with major.minor version") {
    assert(versions.cliScalaVersion("3.6") == Versions.scala36)
  }

  test("get 3Next with full version") {
    assert(versions.cliScalaVersion("3.7.0") == Versions.scala3Next)
  }

  test("get 3Next with major.minor version") {
    assert(versions.cliScalaVersion("3.7") == Versions.scala3Next)
  }

  test("get 3Next with major version") {
    assert(versions.cliScalaVersion("3") == Versions.scala3Next)
  }

}
