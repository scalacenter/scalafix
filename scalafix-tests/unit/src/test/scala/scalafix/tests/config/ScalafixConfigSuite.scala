package scalafix.tests.config

import metaconfig.Conf
import metaconfig.typesafeconfig._
import scalafix.internal.config.ScalafixConfig

class ScalafixConfigSuite extends munit.FunSuite {
  def check(name: String, config: String, expected: ScalafixConfig)(implicit
      loc: munit.Location
  ): Unit = {
    test(name) {
      val obtained = Conf.parseString(config).get.as[ScalafixConfig].get
      assertEquals(obtained, expected)
    }
  }
  check(
    "groupImportsByPrefix",
    "groupImportsByPrefix = true",
    ScalafixConfig(groupImportsByPrefix = true)
  )

  test("dialectOverride allowCaptureChecking") {
    val config = "dialectOverride.allowCaptureChecking = true"
    val obtaind = Conf.parseString(config).get.as[ScalafixConfig].get
    assert(obtaind.dialect.allowCaptureChecking)
  }

  test("dialectOverride invalid key") {
    val config = "dialectOverride.invalidKey = true"
    val obtaind = Conf.parseString(config).get.as[ScalafixConfig].get
    assertEquals(obtaind.dialect, scala.meta.dialects.Scala213)
  }

  test("dialectOverride invalid value") {
    val config = "dialectOverride.allowCaptureChecking = foo"
    val obtaind = Conf.parseString(config).get.as[ScalafixConfig].get
    assertEquals(obtaind.dialect, scala.meta.dialects.Scala213)
  }
}
