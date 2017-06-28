package scalafix.config

import metaconfig._
import MetaconfigParser.parser
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
      ScalafixConfig.ScalafixConfigDecoder
        .read(Input.String(config).toConf.get)
        .get
    }
  }
  check(
    """
      |patches.removeGlobalImports = [
      |  "scala.meta"
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
          RemoveGlobalImport(Symbol("_root_.scala.meta."))
        )
      )
    )
  )
}
