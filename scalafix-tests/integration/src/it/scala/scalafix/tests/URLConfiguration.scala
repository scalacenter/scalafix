package scalafix.tests

import scala.meta.semantic.Database
import scalafix.reflect.ScalafixReflect
import metaconfig.Conf
import org.scalatest.FunSuite

class URLConfiguration extends FunSuite {
  import scalafix.config.ScalafixConfig
  val url =
    "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/193f22e4e2aa624c90fe2ac3bb530b025e104a69/ExampleRewrite.scala"
  test("compile from URL works") {

    val mirror = Some(Database(Nil))
    val obtained =
      ScalafixReflect
        .fromLazyMirror(mirror)
        .read(Conf.Str(url))
    assert(obtained.get.name.contains("Rewrite2"))
  }
}
