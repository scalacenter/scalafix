package scalafix.tests.reflect

import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.reflect.ScalafixCompilerDecoder
import scalafix.internal.tests.utils.SkipWindows
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
  val relpath = RelativePath("NoDummy.scala")
  val abspath: AbsolutePath = cwd.resolve(relpath)
  val decoder: ConfDecoder[Rule] = ScalafixCompilerDecoder.baseCompilerDecoder(
    LazySemanticdbIndex(_ => None, cwd))
  val expectedName = "NoDummy"
  val expectedDescription = ""
  test("absolute path resolves as is", SkipWindows) {
    val rule = decoder.read(Conf.Str(s"file:$abspath")).get
    assert(expectedName == rule.name.value)
  }
  test("relative resolves from custom working directory") {
    val rule = decoder.read(Conf.Str(s"file:$relpath")).get
    assert(expectedName == rule.name.value)
  }
}
