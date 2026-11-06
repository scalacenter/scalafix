package scalafix.tests.config

import scala.meta.io.AbsolutePath

import metaconfig.Conf
import metaconfig.ConfDecoder
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

  test("repeated --classpath arguments are accumulated") {
    val base = Args.default
    implicit val decoder: ConfDecoder[Args] = Args.decoder(base)
    val parsed = Conf
      .parseCliArgs[Args](
        List("--classpath", "a.jar", "--classpath", "b.jar")
      )
      .andThen(_.as[Args])
      .get

    val expected = List(
      base.cwd.resolve("a.jar"),
      base.cwd.resolve("b.jar")
    )
    assertEquals(parsed.classpath.entries, expected)
  }

  test("repeated --classpath arguments with path separator are accumulated") {
    val base = Args.default
    implicit val decoder: ConfDecoder[Args] = Args.decoder(base)
    val parsed = Conf
      .parseCliArgs[Args](
        List(
          "--classpath",
          s"a.jar${java.io.File.pathSeparator}b.jar",
          "--classpath",
          "c.jar"
        )
      )
      .andThen(_.as[Args])
      .get

    val expected = List(
      base.cwd.resolve("a.jar"),
      base.cwd.resolve("b.jar"),
      base.cwd.resolve("c.jar")
    )
    assertEquals(parsed.classpath.entries, expected)
  }

  test("repeated --tool-classpath arguments are accumulated") {
    val base = Args.default
    implicit val decoder: ConfDecoder[Args] = Args.decoder(base)
    val parsed = Conf
      .parseCliArgs[Args](
        List("--tool-classpath", "t1.jar", "--tool-classpath", "t2.jar")
      )
      .andThen(_.as[Args])
      .get

    val expected = List(
      base.cwd.resolve("t1.jar").toNIO.toUri.toURL,
      base.cwd.resolve("t2.jar").toNIO.toUri.toURL
    )
    assertEquals(parsed.toolClasspath.getURLs.toList, expected)
  }
}
