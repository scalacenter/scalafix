package scalafix.tests

import scalafix.SemanticdbIndex
import scalafix.reflect.ScalafixReflect
import metaconfig.Conf
import org.scalatest.FunSuite

class URLConfiguration extends FunSuite {
  import scalafix.internal.config.ScalafixConfig
  val url =
    "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/84dc2450844531d3aeb81d4f3e1dc741abf920c3/ExampleRule.scala"
  test("compile from URL works") {

    val sctx = Some(SemanticdbIndex(Nil))
    val obtained =
      ScalafixReflect
        .fromLazySemanticdbIndex(_ => sctx)
        .read(Conf.Str(url))
    assert(obtained.get.name.contains("Rule2"))
  }
}
