package scalafix.tests.reflect

import scalafix.internal.tests.utils.SkipWindows
import scalafix.internal.v1.Rules
import scalafix.tests.BuildInfo
import metaconfig.Conf
import metaconfig.ConfDecoder
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import org.scalatest.FunSuite
import scalafix.v1.RuleDecoder

class RuleDecoderSuite extends FunSuite {
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
  val decoderSettings = RuleDecoder.Settings().withCwd(cwd)
  val decoder: ConfDecoder[Rules] = RuleDecoder.decoder(decoderSettings)
  val expectedName = "NoDummy"
  val expectedDescription = ""
  test("absolute path resolves as is", SkipWindows) {
    val rules = decoder.read(Conf.Str(s"file:$abspath")).get
    assert(expectedName == rules.name.value)
  }
  test("relative resolves from custom working directory") {
    val rules = decoder.read(Conf.Str(s"file:$relpath")).get
    assert(expectedName == rules.name.value)
  }
}
