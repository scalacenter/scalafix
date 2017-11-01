package scalafix.tests.reflect

import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.reflect.ScalafixCompilerDecoder
import scalafix.rule.Rule
import scalafix.tests.BuildInfo
import metaconfig.Conf
import metaconfig.ConfDecoder
import org.langmeta.io.AbsolutePath
import org.langmeta.io.RelativePath
import org.scalatest.FunSuite

class ScalafixReflectTests extends FunSuite {
  val cwd: AbsolutePath = AbsolutePath(BuildInfo.baseDirectory)
    .resolve("scalafix-tests")
    .resolve("unit")
    .resolve("src")
    .resolve("main")
    .resolve("scala")
    .resolve("scalafix")
    .resolve("test")
  val relpath = RelativePath("DummyLinter.scala")
  val abspath: AbsolutePath = cwd.resolve(relpath)
  val decoder: ConfDecoder[Rule] = ScalafixCompilerDecoder.baseCompilerDecoder(
    LazySemanticdbIndex(_ => None, cwd))
  val expected = "DummyLinter"
  test("absolute path resolves as is") {
    val obtained = decoder.read(Conf.Str(s"file:$abspath")).get.name.value
    assert(expected == obtained)
  }
  test("relative resolves from custom working directory") {
    val obtained = decoder.read(Conf.Str(s"file:$relpath")).get.name.value
    assert(expected == obtained)
  }
}
