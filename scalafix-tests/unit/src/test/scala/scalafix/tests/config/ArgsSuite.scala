package scalafix.tests.config

import metaconfig.Conf
import metaconfig.internal.ConfGet
import scalafix.internal.v1.Args
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalafixConfig

class ArgsSuite extends munit.FunSuite {

  private lazy val onCompileConf = Conf.parseString(
    "ArgsSuite",
    """
      |rules = [DisableSyntax, RemoveUnused]
      |
      |onCompile.rules = [DisableSyntax]
      |
      |DisableSyntax.noVars = true
      |DisableSyntax.noThrows = true
      |
      |onCompile = {
      |  DisableSyntax.noVars = false
      |}
      |
      |onCompile.DisableSyntax.noReturns = true
      |""".stripMargin
  )

  test("ignore onCompile section if args.onCompile is false") {
    val args = Args.default.copy(scalacOptions = "-Ywarn-unused" :: Nil)
    val config = ScalafixConfig()

    assert(!args.onCompile, "onCompile should be false at default.")

    val rules = args.configuredRules(onCompileConf.get, config).get

    assert(
      rules.rules.map(_.name.value) == List("DisableSyntax", "RemoveUnused")
    )

    val merged = args.preProcessedConf(onCompileConf.get)

    val disableSyntaxRule = ConfGet.getKey(merged, "DisableSyntax" :: Nil).get

    val expected =
      Conf.Obj("noVars" -> Conf.Bool(true), "noThrows" -> Conf.Bool(true))

    assertEquals(disableSyntaxRule, expected)
  }

  test("use onCompile section if args.onCompile is true") {
    val args = Args.default.copy(onCompile = true)
    val config = ScalafixConfig()

    val rules = args.configuredRules(onCompileConf.get, config).get

    assert(rules.rules.map(_.name.value) == List("DisableSyntax"))

    val merged = args.preProcessedConf(onCompileConf.get)

    val disableSyntaxRule = ConfGet.getKey(merged, "DisableSyntax" :: Nil).get

    val expected =
      Conf.Obj("noVars" -> Conf.Bool(false), "noReturns" -> Conf.Bool(true))

    assertEquals(disableSyntaxRule, expected)
  }
}
