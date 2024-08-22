package scalafix.tests.reflect

import java.net.URLClassLoader
import java.nio.file.Files

import scala.meta.io.AbsolutePath

import coursier._
import metaconfig.Conf
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import scalafix.Versions
import scalafix.internal.config.ScalaVersion
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.reflect.RuleCompilerClasspath
import scalafix.v1.RuleDecoder

class ToolClasspathSuite extends AnyFunSuite with BeforeAndAfterAll {
  var scalaClasspath: List[AbsolutePath] = _
  override def beforeAll(): Unit = {
    val scalaBinaryVersion =
      ScalaVersion.from(Versions.scalaVersion).get.binary.get.value
    val jars =
      Fetch()
        .addDependencies(
          Dependency(
            Module(
              Organization("org.typelevel"),
              ModuleName(s"cats-core_${scalaBinaryVersion}")
            ),
            "2.10.0"
          )
        )
        .run()
        .toList

    scalaClasspath = jars.map(AbsolutePath(_))
  }

  test("--tool-classpath is respected when compiling from source") {
    val sourceRule =
      """
        |import scalafix.v1._
        |
        |import cats.Functor
        |import cats.implicits._
        |
        |class CatsRule extends SyntacticRule("CatsRule") {
        |  val optionFunctor = Functor[Option]
        |  val result = optionFunctor.map(Some(2))(_ + 3)
        |  println(result)
        |}
      """.stripMargin
    val tmpFile = Files.createTempFile("scalafix", "CatsRule.scala")
    Files.write(tmpFile, sourceRule.getBytes)
    val decoderSettings =
      RuleDecoder.Settings().withToolClasspath(scalaClasspath)
    val decoder = RuleDecoder.decoder(decoderSettings)
    val obtained = decoder.read(Conf.Str(tmpFile.toUri.toString)).get
    val expectedName = "CatsRule"
    assert(obtained.name.value == expectedName)
  }

  test("--tool-classpath is respected during classloading") {
    val rewrite =
      """package custom
        |import scalafix.v1._
        |class CustomRule extends SyntacticRule("CustomRule")
      """.stripMargin
    val tmp = Files.createTempDirectory("scalafix")
    val compiler = new RuleCompiler(
      new URLClassLoader(
        RuleCompilerClasspath.defaultClasspathPaths
          .map(_.toNIO.toUri.toURL)
          .toArray,
        getClass.getClassLoader
      ),
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

}
