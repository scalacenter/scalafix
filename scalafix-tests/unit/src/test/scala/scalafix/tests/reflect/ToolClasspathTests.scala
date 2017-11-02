package scalafix.tests.reflect

import java.io.File
import java.nio.file.Files
import scala.reflect.io.Directory
import scala.reflect.io.PlainDirectory
import scalafix.internal.config.LazySemanticdbIndex
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.reflect.ScalafixCompilerDecoder
import scalafix.testkit.utest.ScalafixTest
import metaconfig.Conf
import org.langmeta.inputs.Input
import org.langmeta.io.AbsolutePath

object ToolClasspathTests extends ScalafixTest {
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

  test("--tool-classpath is respected when compiling from source") {
    val scalafmtRewrite =
      """
        |import org.scalafmt._
        |import scalafix._
        |
        |object FormatRule extends Rule("FormatRule") {
        |  override def fix(ctx: RuleCtx): Patch = {
        |    val formatted = Scalafmt.format(ctx.tokens.mkString).get
        |    ctx.addLeft(ctx.tokens.last, formatted)
        |  }
        |}
      """.stripMargin
    val tmp = Files.createTempFile("scalafix", "FormatRule.scala")
    Files.write(tmp, scalafmtRewrite.getBytes)
    val index =
      new LazySemanticdbIndex(toolClasspath = scalafmtClasspath)
    val decoder = ScalafixCompilerDecoder.baseCompilerDecoder(index)
    val obtained = decoder.read(Conf.Str(s"file:$tmp")).get
    val expected = "FormatRule"
    assert(obtained.name.value == expected)
  }

  test("--tool-classpath is respected during classloading") {
    // Couldn't figure out how to test this.
    val rewrite =
      """package custom
        |import scalafix._
        |class CustomRule extends Rule("CustomRule")
      """.stripMargin
    val tmp = Files.createTempDirectory("scalafix")
    val compiler = new RuleCompiler(
      RuleCompiler.defaultClasspath,
      new PlainDirectory(new Directory(tmp.toFile)))
    compiler.compile(Input.VirtualFile("CustomRule.scala", rewrite))
    val index =
      new LazySemanticdbIndex(toolClasspath = AbsolutePath(tmp) :: Nil)
    val decoder = ScalafixMetaconfigReaders.classloadRuleDecoder(index)
    val obtained = decoder.read(Conf.Str(s"class:custom.CustomRule")).get
    val expected = "CustomRule"
    assert(obtained.name.value == expected)
    assert(decoder.read(Conf.Str("class:does.not.Exist")).isNotOk)
  }

}
