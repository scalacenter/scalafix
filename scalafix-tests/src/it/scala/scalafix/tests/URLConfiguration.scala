package scalafix.tests

import scalafix.testkit._

class URLConfiguration
    extends SemanticRewriteSuite(SemanticRewriteSuite.thisClasspath) {
  import scalafix.config.ScalafixConfig
  val url =
    "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/ff0a1a8c71936bb3d06f804274dc042131fd8392/ExampleRewrite.scala"
  val code =
    s"""
       |rewrites = [
       |  "$url"
       |]
       |<<< NOWRAP basic
       |object Name
       |>>>
       |import scala.collection.immutable.Seq
       |object Name1
       |
    """.stripMargin
  DiffTest("url", code, "UrlRewrite.stat").foreach(runDiffTest)
}
