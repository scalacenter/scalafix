package scalafix.tests.config

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.rule.RemoveUnused
import scalafix.v1.Configuration

class RemoveUnusedConfigSuite extends AnyFunSuite {

  private def check(scalaVersion: String, expectedOk: Boolean): Unit = {
    val config = Configuration()
      .withScalaVersion(scalaVersion)
      .withScalacOptions(List("-Wunused:all"))
    val result = new RemoveUnused().withConfiguration(config)
    assert(
      result.isOk == expectedOk,
      s"RemoveUnused with Scala $scalaVersion: expected isOk=$expectedOk, got $result"
    )
  }

  test("RemoveUnused accepts Scala 3.10.0-RC1") {
    check("3.10.0-RC1", expectedOk = true)
  }

  test("RemoveUnused accepts Scala 3.3.4") {
    check("3.3.4", expectedOk = true)
  }

  test("RemoveUnused accepts Scala 2.13.18") {
    check("2.13.18", expectedOk = true)
  }

  test("RemoveUnused rejects Scala 3.3.3") {
    check("3.3.3", expectedOk = false)
  }

  test("RemoveUnused rejects Scala 3.1.3") {
    check("3.1.3", expectedOk = false)
  }
}
