package scalafix.tests.reflect

import java.io.File
import java.nio.file.Files
import scala.reflect.io.Directory
import scala.reflect.io.PlainDirectory
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.tests.utils.SkipWindows
import metaconfig.Conf
import scala.meta.io.AbsolutePath
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import scalafix.v1.RuleDecoder

class ToolClasspathTests extends FunSuite with BeforeAndAfterAll {
  var scalafmtClasspath: List[AbsolutePath] = _
  override def beforeAll(): Unit = {
    val scalaBinaryVersion =
      scala.util.Properties.versionNumberString
        .split("\\.")
        .take(2)
        .mkString(".")
    val jars: List[File] = scalafix.internal.sbt.ScalafixJarFetcher.fetchJars(
      "com.geirsson",
      "scalafmt-core_" + scalaBinaryVersion,
      "1.2.0"
    )
    scalafmtClasspath = jars.map(AbsolutePath(_))
  }

  test("--tool-classpath is respected when compiling from source", SkipWindows) {
    val scalafmtRewrite =
      """
        |import org.scalafmt._
        |import scalafix.v0._
        |
        |object FormatRule extends Rule("FormatRule") {
        |  override def description: String = "FormatRuleDescription"
        |  override def fix(ctx: RuleCtx): Patch = {
        |    val formatted = Scalafmt.format(ctx.tokens.mkString).get
        |    ctx.addLeft(ctx.tokens.last, formatted)
        |  }
        |}
      """.stripMargin
    val tmp = Files.createTempFile("scalafix", "FormatRule.scala")
    Files.write(tmp, scalafmtRewrite.getBytes)
    val decoderSettings =
      RuleDecoder.Settings().withToolClasspath(scalafmtClasspath)
    val decoder = RuleDecoder.decoder(decoderSettings)
    val obtained = decoder.read(Conf.Str(s"file:$tmp")).get
    val expectedName = "FormatRule"
    assert(obtained.name.value == expectedName)
  }

  test("--tool-classpath is respected during classloading") {
    val rewrite =
      """package custom
        |import scalafix.v0._
        |class CustomRule extends Rule("CustomRule")
      """.stripMargin
    val tmp = Files.createTempDirectory("scalafix")
    val compiler = new RuleCompiler(
      RuleCompiler.defaultClasspath,
      new PlainDirectory(new Directory(tmp.toFile)))
    compiler.compile(metaconfig.Input.VirtualFile("CustomRule.scala", rewrite))
    val decoderSettings =
      RuleDecoder.Settings().withToolClasspath(AbsolutePath(tmp) :: Nil)
    val decoder = RuleDecoder.decoder(decoderSettings)
    val obtained = decoder.read(Conf.Str(s"class:custom.CustomRule")).get
    val expectedName = "CustomRule"
    assert(obtained.name.value == expectedName)
    assert(decoder.read(Conf.Str("class:does.not.Exist")).isNotOk)
  }

}
