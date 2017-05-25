package scalafix.tests

import scalafix.testkit._

class URLConfiguration
    extends SemanticRewriteSuite(SemanticRewriteSuite.thisClasspath) {
  import scalafix.config.ScalafixConfig
  val url =
    "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/193f22e4e2aa624c90fe2ac3bb530b025e104a69/ExampleRewrite.scala"
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
