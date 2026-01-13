package scalafix.tests.rules

import metaconfig.Conf
import metaconfig.Configured
import metaconfig.typesafeconfig._
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.rule.OrganizeImports
import scalafix.v1.Configuration

class OrganizeImportsSuite extends AnyFunSuite {
  val rule = new OrganizeImports()

  test("OrganizeImports should fail when RemoveUnused.imports is enabled") {
    val conf = Conf
      .parseString(
        """OrganizeImports.removeUnused = true
          |RemoveUnused.imports = true
          |""".stripMargin
      )
      .get
    
    val config = Configuration()
      .withConf(conf)
      .withScalacOptions(List("-Wunused"))
    
    val result = rule.withConfiguration(config)
    
    assert(result.isNotOk, "Expected OrganizeImports to fail with RemoveUnused.imports=true")
    
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

  test("OrganizeImports should succeed when RemoveUnused.imports is false") {
    val conf = Conf
      .parseString(
        """OrganizeImports.removeUnused = true
          |RemoveUnused.imports = false
          |""".stripMargin
      )
      .get
    
    val config = Configuration()
      .withConf(conf)
      .withScalacOptions(List("-Wunused"))
    
    val result = rule.withConfiguration(config)
    
    assert(result.isOk, s"Expected OrganizeImports to succeed with RemoveUnused.imports=false, got: $result")
  }

  test("OrganizeImports should succeed when RemoveUnused is not configured") {
    val conf = Conf
      .parseString("OrganizeImports.removeUnused = true")
      .get
    
    val config = Configuration()
      .withConf(conf)
      .withScalacOptions(List("-Wunused"))
    
    val result = rule.withConfiguration(config)
    
    assert(result.isOk, s"Expected OrganizeImports to succeed when RemoveUnused is not configured, got: $result")
  }
}
