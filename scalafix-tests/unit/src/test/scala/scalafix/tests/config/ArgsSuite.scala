package scalafix.tests.config

import scala.meta.io.AbsolutePath

import metaconfig.Conf
import metaconfig.Configured
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalaVersion
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.v1.Args

class ArgsSuite extends munit.FunSuite {

  private lazy val givenConf = Conf
    .parseString(
      "ArgsSuite",
      """
        |rules = [DisableSyntax, RemoveUnused]
        |
        |triggered.rules = [DisableSyntax]
        |
        |DisableSyntax.noVars = true
        |DisableSyntax.noThrows = true
        |
        |triggered = {
        |  DisableSyntax.noVars = false
        |}
        |
        |triggered.DisableSyntax.noReturns = true
        |""".stripMargin
    )
    .get

  test("ignore triggered section if args.triggered is false") {
    val args = Args.default.copy(scalacOptions = "-Ywarn-unused" :: Nil)
    val config = ScalafixConfig()

    assert(!args.triggered, "triggered should be false at default.")

    val rulesConfigured = args.configuredRules(givenConf, config).get

    assert(
      rulesConfigured.rules
        .map(_.name.value) == List("DisableSyntax", "RemoveUnused")
    )

    val merged = args.maybeOverlaidConfWithTriggered(givenConf)

    val disableSyntaxRule = ConfGet
      .getOrOK(merged, "DisableSyntax" :: Nil, Configured.ok, Conf.Obj.empty)
      .get

    val expected =
      Conf.Obj("noVars" -> Conf.Bool(true), "noThrows" -> Conf.Bool(true))

    assertEquals(disableSyntaxRule, expected)
  }

  test("use triggered section if args.triggered is true") {
    val args = Args.default.copy(triggered = true)
    val config = ScalafixConfig()

    val rulesConfigured = args.configuredRules(givenConf, config).get

    assert(rulesConfigured.rules.map(_.name.value) == List("DisableSyntax"))

    val merged = args.maybeOverlaidConfWithTriggered(givenConf)

    val disableSyntaxRule = ConfGet
      .getOrOK(merged, "DisableSyntax" :: Nil, Configured.ok, Conf.Obj.empty)
      .get

    val expected =
      Conf.Obj(
        "noVars" -> Conf.Bool(false),
        "noThrows" -> Conf.Bool(true),
        "noReturns" -> Conf.Bool(true)
      )

    assertEquals(disableSyntaxRule, expected)
  }

  test("targetRoot") {
    val targetRootPath = "/some-path"
    val scalacOptions = List("first", "-semanticdb-target", targetRootPath)
    val args: Args = Args.default.copy(
      scalacOptions = scalacOptions,
      scalaVersion = ScalaVersion.from("3.0.0-RC3").get
    )
    val classpath = args.validatedClasspath
    assert(classpath.entries.contains(AbsolutePath(targetRootPath)))
  }

  test("scala-library is resolved when scalaVersion is fully specified") {
    val args: Args = Args.default.copy(
      scalaVersion = ScalaVersion.from("2.13.10").get
    )
    val classpath = args.validatedClasspath
    assert(
      classpath.entries.exists(
        _.toNIO.getFileName.toString.contains("scala-library")
      ),
      "scala-library should be in classpath when scalaVersion is fully specified"
    )
  }

  test("scala-library is not duplicated when already in classpath") {
    val scalaLibJar =
      AbsolutePath("/fake/path/scala-library.jar")

    val args: Args = Args.default.copy(
      scalaVersion = ScalaVersion.from("2.13.10").get,
      classpath = scalaLibJar :: Nil
    )

    val classpath = args.validatedClasspath

    val scalaLibCount =
      classpath.entries.count(
        _.toNIO.getFileName.toString.contains("scala-library")
      )

    assertEquals(
      scalaLibCount,
      1,
      "scala-library should not be duplicated if already provided by the user"
    )
  }

  test(
    "scala3-library is resolved when Scala 3 scalaVersion is fully specified"
  ) {
    val args: Args = Args.default.copy(
      scalaVersion = ScalaVersion.from("3.3.1").get
    )
    val classpath = args.validatedClasspath
    assert(
      classpath.entries.exists(
        _.toNIO.getFileName.toString.contains("scala3-library")
      ),
      "scala3-library should be in classpath when Scala 3 version is fully specified"
    )
  }

  test("scala-library is not resolved when scalaVersion is partial") {
    val args: Args = Args.default.copy(
      scalaVersion = ScalaVersion.from("2.13").get
    )
    val classpath = args.validatedClasspath
    assert(
      !classpath.entries.exists(
        _.toNIO.getFileName.toString.contains("scala-library")
      ),
      "scala-library should not be auto-resolved when scalaVersion is partial"
    )
  }
}
