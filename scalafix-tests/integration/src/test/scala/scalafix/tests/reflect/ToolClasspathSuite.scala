package scalafix.tests.reflect

import java.nio.file.Files

import scala.meta.io.AbsolutePath

import coursier._
import metaconfig.Conf
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import scalafix.Versions
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.reflect.RuleCompilerClasspath
import scalafix.internal.tests.utils.SkipWindows
import scalafix.v1.RuleDecoder


import scala.tools.nsc.CloseableRegistry
import scala.tools.nsc.classpath.JrtClassPath
import scala.util.Properties

class ToolClasspathSuite extends AnyFunSuite with BeforeAndAfterAll {
  var scalaClasspath: List[AbsolutePath] = _
  override def beforeAll(): Unit = {
    var scalaVersionSuffix: String = ""
    val versionSplit = Versions.scalaVersion.split("\\.")
    if (versionSplit(0) == "3") {
      scalaVersionSuffix = versionSplit(0)
    } else {
      scalaVersionSuffix = s"${versionSplit(0)}.${versionSplit(1)}"
    }
    val jars =
      Fetch()
        .addDependencies(
          Dependency(
            Module(
              Organization("org.scalatest"),
              ModuleName(s"scalatest_${scalaVersionSuffix}")
            ),
            "3.2.13"
          )
        )
        .run()
        .toList

    scalaClasspath = jars.map(AbsolutePath(_))
  }

  test(
    "--tool-classpath is respected when compiling from source",
    SkipWindows
  ) {
    val scalaRewrite =
      """
        |import org.scalatest.Assertions._
        |import scalafix.v0._
        |
        |object FormatRule extends Rule("FormatRule")
        | {
        |  override def description: String = "RuleDescription"
        |  override def fix(ctx: RuleCtx): Patch = {
        |    assert("raz" == "raz")
        |    ctx.addLeft(ctx.tokens.last, "test")
        |  }
        |}
      """.stripMargin
    val tmpFile = Files.createTempFile("scalafix", "FormatRule.scala")
    Files.write(tmpFile, scalaRewrite.getBytes)
    val decoderSettings =
      RuleDecoder.Settings().withToolClasspath(scalaClasspath)
    val decoder = RuleDecoder.decoder(decoderSettings)
    val obtained = decoder.read(Conf.Str(s"file:$tmpFile")).get
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
      RuleCompilerClasspath.defaultClasspath,
      Some(tmp.toFile)
    )
    compiler.compile(metaconfig.Input.VirtualFile("CustomRule.scala", rewrite))
    val decoderSettings =
      RuleDecoder.Settings().withToolClasspath(AbsolutePath(tmp) :: Nil)
    val decoder = RuleDecoder.decoder(decoderSettings)
    val obtained = decoder.read(Conf.Str(s"class:custom.CustomRule")).get
    val expectedName = "CustomRule"
    assert(obtained.name.value == expectedName)
    assert(decoder.read(Conf.Str("class:does.not.Exist")).isNotOk)
  }

  test("jrt") {
    val version = scala.reflect.internal.JDK9Reflectors.runtimeVersionMajor(scala.reflect.internal.JDK9Reflectors.runtimeVersion()).intValue()
    println(s"reflectors: $version")

    println(Properties.javaHome)

    val closeableRegistry = new CloseableRegistry
    val cp = JrtClassPath(None, closeableRegistry).get
    assert(cp.findClass("sun.misc.Unsafe").isDefined)
  }

}
