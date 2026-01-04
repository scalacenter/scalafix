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
    val obtained = Conf.parseString(config).get.as[ScalafixConfig].get
    assert(obtained.dialect.allowCaptureChecking)
  }

  test("dialectOverride invalid key") {
    val config = "dialectOverride.invalidKey = true"
    val obtained = Conf.parseString(config).get.as[ScalafixConfig].get
    assertEquals(obtained.dialect, scala.meta.dialects.Scala213)
  }

  test("dialectOverride invalid value") {
    val config = "dialectOverride.allowCaptureChecking = foo"
    val error = intercept[NoSuchElementException] {
      Conf.parseString(config).get.as[ScalafixConfig].get
    }
    assert(error.getMessage.contains("error: Type mismatch"))
  }
}
