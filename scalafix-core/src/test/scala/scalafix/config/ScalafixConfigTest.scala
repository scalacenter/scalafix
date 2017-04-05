package scalafix.config

import scalafix.util.TreePatch.Replace
import scala.meta._
import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.RemoveGlobalImport

import metaconfig.ConfDecoder
import metaconfig.hocon.Hocon2Class
import org.scalameta.logger
import org.scalatest.FunSuite

class ScalafixConfigTest extends FunSuite {
  val default = ScalafixConfig.default
  implicit val reader = ScalafixConfig.default.reader
  def check[T](config: String, expected: T)(
      implicit ev: ConfDecoder[T]): Unit = {
    test(logger.revealWhitespace(config).take(50)) {
      Hocon2Class.gimmeClass[T](config, ev, None) match {
        case Right(obtained) =>
          AnyEqual.assertEqual(obtained, expected)
        case Left(e) => throw e
      }
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
