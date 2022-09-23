package scalafix.tests.reflect

import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath

import metaconfig.Conf
import metaconfig.ConfDecoder
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.tests.utils.SkipWindows
import scalafix.internal.v1.Rules
import scalafix.tests.BuildInfo
import scalafix.v1.RuleDecoder
import java.nio.file.Files

class RuleDecoderSuite extends AnyFunSuite {
  val cwd: AbsolutePath = AbsolutePath(BuildInfo.baseDirectory)
    .resolve("scalafix-tests")
    .resolve("unit")
    .resolve("src")
    .resolve("main")
    .resolve("scala")
    .resolve("scalafix")
    .resolve("test")
  val relpath: RelativePath = RelativePath("NoDummy.scala")
  val abspath: AbsolutePath = cwd.resolve(relpath)
  val decoderSettings: RuleDecoder.Settings =
    RuleDecoder.Settings().withCwd(cwd)
  val decoder: ConfDecoder[Rules] = RuleDecoder.decoder(decoderSettings)
  val expectedName = "NoDummy"

  test("absolute path resolves as is", SkipWindows) {
    val rules = decoder.read(Conf.Str(s"file:$abspath")).get
    assert(expectedName == rules.name.value)
  }

  test("relative resolves from custom working directory") {
    val rules = decoder.read(Conf.Str(s"file:$relpath")).get
    assert(expectedName == rules.name.value)
  }

  test("resolved classes can be reloaded", SkipWindows) {
    val tmp = Files.createTempFile("scalafix", "CustomRule.scala")

    val customRuleV1 =
      """package custom
        |import scalafix.v1._
        |class CustomRule extends SyntacticRule("CustomRule") {}
      """.stripMargin
    Files.write(tmp, customRuleV1.getBytes)
    val rules1 =
      decoder.read(Conf.Str(s"file:${tmp.toFile.getAbsolutePath}")).get
    val class1 = rules1.rules.head.getClass

    val customRuleV2 =
      """package custom
        |import scalafix.v1._
        |class CustomRule extends SyntacticRule("CustomRule") {
        |  def foo = 1
        |}
      """.stripMargin
    Files.write(tmp, customRuleV2.getBytes)
    val rules2 =
      decoder.read(Conf.Str(s"file:${tmp.toFile.getAbsolutePath}")).get
    val class2 = rules2.rules.head.getClass

    assert(!class1.isAssignableFrom(class2))
  }
}
