package scalafix.rewrite

import scala.meta.inputs.Input
import scalafix.Scalafix
import scalafix.config.ProjectFiles
import scalafix.config.ScalafixConfig
import scalafix.util.DiffAssertions

import java.io.File

import org.scalatest.FunSuite

class ExplicitImplicitTest extends FunSuite with DiffAssertions {

  def check(target: String, filename: String): String = {
    val config = ScalafixConfig(
      rewrites = Seq(ExplicitImplicit),
      project = ProjectFiles(
        targetFiles = Seq(new File(target))
      )
    )
    val Right(output) = Scalafix.fix(Input.File(filename), config)
    output
  }
  test("basic") {
    val obtained =
      check("../semanticCompile/target",
            "../semanticCompile/src/main/scala/semantic/CompileMe.scala")
    val expected =
      """|package semantic
         |
         |class Goodbye
         |
         |class CompileMe() {
         |  implicit val x: scala.Int = 1
         |  implicit val goodbye: semantic.Goodbye = new Goodbye
         |}
         |
         |object CompileMe {
         |  implicit val y = "string"
         |  implicit def method = new CompileMe
         |}
         |""".stripMargin.trim
    assertNoDiff(obtained, expected)
  }
}

