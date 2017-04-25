package scalafix.config

import scala.meta._
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.RemoveGlobalImport
import scalafix.patch.TreePatch.Replace

import org.scalameta.logger
import org.scalatest.FunSuite

class ScalafixConfigTest extends FunSuite {
  val default = ScalafixConfig.default
  implicit val reader = ScalafixConfig.default.reader
  def check[T](config: String, expected: T): Unit = {
    test(logger.revealWhitespace(config).take(50)) {
      ScalafixConfig.fromString(config, None)(rewriteConfDecoder(None)).get
    }
  }
  check(
    """
      |patches.removeGlobalImports = [
      |  "scala.{meta => m}"
      |]
      |patches.addGlobalImports = [
      |  "scala.meta._"
      |]
      |patches.replacements = [{
      |    from = _root_.org.scalameta.
      |    to = scala.meta
      |    additionalImports = [
      |      scala.meta._
      |    ]
      |}]
      |imports.organize = false
      |imports.groups = [
      |  foo.bar
      |]
    """.stripMargin,
    default.copy(
      patches = default.patches.copy(
        replacements = List(
          Replace(Symbol("_root_.org.scalameta."),
                  to = q"scala.meta",
                  additionalImports = List(
                    importer"scala.meta._"
                  ))
        ),
        addGlobalImports = List(
          AddGlobalImport(importer"scala.meta._")
        ),
        removeGlobalImports = List(
          RemoveGlobalImport(importer"scala.{meta => m}")
        )
      ),
      imports = default.imports.copy(
        organize = false,
        groups = List(
          FilterMatcher("foo.bar")
        )
      )
    )
  )
}
