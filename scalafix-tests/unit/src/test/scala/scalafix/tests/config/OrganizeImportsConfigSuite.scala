package scalafix.tests.config

import metaconfig.Conf
import metaconfig.Configured
import metaconfig.typesafeconfig._
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.rule.OrganizeImports
import scalafix.v1.Configuration

class OrganizeImportsConfigSuite extends AnyFunSuite {

  test("OrganizeImports should fail when RemoveUnused.imports is enabled") {
    val rawConfig =
      """|rules = [OrganizeImports]
        |OrganizeImports.removeUnused = true
        |RemoveUnused.imports = true
        |""".stripMargin

    val conf = Conf.parseString("test", rawConfig).get
    val config =
      Configuration().withConf(conf).withScalacOptions(List("-Wunused"))
    val rule = new OrganizeImports()

    val result = rule.withConfiguration(config)

    assert(
      result.isNotOk,
      "Expected OrganizeImports to fail with RemoveUnused.imports=true"
    )

    result match {
      case Configured.NotOk(error) =>
        val errorMsg = error.msg
        assert(
          errorMsg.contains("RemoveUnused.imports") &&
            errorMsg.contains("OrganizeImports") &&
            errorMsg.contains("should not be used together"),
          s"Error message should mention the conflict. Got: $errorMsg"
        )
      case _ =>
        fail("Expected Configured.NotOk")
    }
  }

  test("OrganizeImports should succeed when RemoveUnused.imports is disabled") {
    val rawConfig =
      """|rules = [OrganizeImports]
        |OrganizeImports.removeUnused = true
        |RemoveUnused.imports = false
        |""".stripMargin

    val conf = Conf.parseString("test", rawConfig).get
    val config =
      Configuration().withConf(conf).withScalacOptions(List("-Wunused"))
    val rule = new OrganizeImports()

    val result = rule.withConfiguration(config)

    assert(
      result.isOk,
      "Expected OrganizeImports to succeed with RemoveUnused.imports=false"
    )
  }

  test("OrganizeImports should succeed when RemoveUnused is not configured") {
    val rawConfig =
      """|rules = [OrganizeImports]
        |OrganizeImports.removeUnused = true
        |""".stripMargin

    val conf = Conf.parseString("test", rawConfig).get
    val config =
      Configuration().withConf(conf).withScalacOptions(List("-Wunused"))
    val rule = new OrganizeImports()

    val result = rule.withConfiguration(config)

    assert(
      result.isOk,
      "Expected OrganizeImports to succeed when RemoveUnused is not configured"
    )
  }

  private def checkRemoveUnusedScalaVersion(
      scalaVersion: String,
      expectedOk: Boolean
  ): Unit = {
    val rawConfig =
      """|rules = [OrganizeImports]
        |OrganizeImports.removeUnused = true
        |""".stripMargin
    val conf = Conf.parseString("test", rawConfig).get
    val config = Configuration()
      .withConf(conf)
      .withScalaVersion(scalaVersion)
      .withScalacOptions(List("-Wunused:all"))
    val result = new OrganizeImports().withConfiguration(config)
    assert(
      result.isOk == expectedOk,
      s"OrganizeImports.removeUnused with Scala $scalaVersion: expected isOk=$expectedOk, got $result"
    )
    if (!expectedOk) {
      result match {
        case Configured.NotOk(error) =>
          assert(
            error.msg.contains("not supported") &&
              error.msg.contains("3.3.4"),
            s"Error should mention unsupported compiler. Got: ${error.msg}"
          )
        case _ => fail("Expected Configured.NotOk")
      }
    }
  }

  test("OrganizeImports.removeUnused accepts Scala 3.10.0-RC1") {
    checkRemoveUnusedScalaVersion("3.10.0-RC1", expectedOk = true)
  }

  test("OrganizeImports.removeUnused accepts Scala 3.3.4") {
    checkRemoveUnusedScalaVersion("3.3.4", expectedOk = true)
  }

  test("OrganizeImports.removeUnused accepts Scala 2.13.18") {
    checkRemoveUnusedScalaVersion("2.13.18", expectedOk = true)
  }

  test("OrganizeImports.removeUnused rejects Scala 3.3.3") {
    checkRemoveUnusedScalaVersion("3.3.3", expectedOk = false)
  }

  test("OrganizeImports.removeUnused rejects Scala 3.1.3") {
    checkRemoveUnusedScalaVersion("3.1.3", expectedOk = false)
  }
}
