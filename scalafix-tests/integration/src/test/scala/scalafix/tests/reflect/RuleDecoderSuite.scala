package scalafix.tests.reflect

import java.nio.file.Files

import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath

import metaconfig.Conf
import metaconfig.ConfDecoder
import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.tests.utils.SkipWindows
import scalafix.internal.v1.Rules
import scalafix.tests.BuildInfo
import scalafix.v1.RuleDecoder

class RuleDecoderSuite extends AnyFunSuite {
  val cwd: AbsolutePath = AbsolutePath(BuildInfo.baseDirectory)
    .resolve("scalafix-tests")
    .resolve("integration")
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

  test("absolute path resolves as URI") {
    val rules = decoder.read(Conf.Str(abspath.toURI.toString)).get
    assert(expectedName == rules.name.value)
  }

  test(
    "absolute path resolves as is",
    // heading slashes are needed on Windows, see https://en.wikipedia.org/wiki/File_URI_scheme#Examples
    SkipWindows
  ) {
    val rules = decoder.read(Conf.Str(s"file:$abspath")).get
    assert(expectedName == rules.name.value)
  }

  test("relative resolves from custom working directory") {
    val rules = decoder.read(Conf.Str(s"file:$relpath")).get
    assert(expectedName == rules.name.value)
  }

  test("resolved classes can be reloaded") {
    val tmp = Files.createTempFile("scalafix", "CustomRule.scala")

    val customRuleV1 =
      """package custom
        |import scalafix.v1._
        |class CustomRule extends SyntacticRule("CustomRule") {}
      """.stripMargin
    Files.write(tmp, customRuleV1.getBytes)
    val rules1 =
      decoder.read(Conf.Str(tmp.toUri.toString)).get
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
      decoder.read(Conf.Str(tmp.toUri.toString)).get
    val class2 = rules2.rules.head.getClass

    assert(!class1.isAssignableFrom(class2))
  }

  /*
  test("deprecation warnings do not prevent rule loading") {
    val tmp = Files.createTempFile("scalafix", "CustomRule.scala")

    val deprecatedRule =
      """package custom
        |import scala.meta._
        |import scalafix.v1._
        |class DeprecatedRule extends SyntacticRule("DeprecatedRule") {
        |  override def fix(implicit doc: SyntacticDocument) =
        |    doc.tree.collect {
        |      // deprecated https://github.com/scalameta/scalameta/blob/a8eb596/scalameta/trees/shared/src/main/scala/scala/meta/Trees.scala#L691
        |      case Type.And(_, _) => Patch.empty
        |    }.asPatch
        |}
      """.stripMargin
    Files.write(tmp, deprecatedRule.getBytes)

    val rules =
      decoder.read(Conf.Str(tmp.toUri.toString)).get
    assert(rules.rules.nonEmpty)
  }
   */

  test("compilation errors are properly exposed") {
    val tmp = Files.createTempFile("scalafix", "BrokenRule.scala")

    val brokenRule =
      """package custom
        |import scalafix.v1._
        |class BrokenRule extends SyntacticRule("BrokenRule") {
        |  val broken: String = 123 // type mismatch error
        |}
      """.stripMargin
    Files.write(tmp, brokenRule.getBytes)

    val result = decoder.read(Conf.Str(tmp.toUri.toString))
    assert(result.isNotOk)

    val error = result.toEither.left.get
    assert(error.msg.contains("type mismatch"))
  }
}
