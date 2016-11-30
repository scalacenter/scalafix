package scalafix

import scala.collection.immutable.Seq
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.reporters.StoreReporter
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.{meta => m}
import scalafix.nsc.NscSemanticApi
import scalafix.rewrite.ExplicitImplicit
import scalafix.rewrite.Rewrite

import org.scalatest.FunSuite

abstract class NscPluginSuite(rewrite: Rewrite,
                              parseAsCompilationUnit: Boolean = false)
    extends FunSuite {

  private lazy val g: Global = {
    def fail(msg: String) =
      sys.error(s"ReflectToMeta initialization failed: $msg")
    val classpath = System.getProperty("sbt.paths.scalafixNsc.test.classes")
    val pluginpath = System.getProperty("sbt.paths.plugin.jar")
    val options = "-cp " + classpath + " -Xplugin:" + pluginpath + ":" + classpath + " -Xplugin-require:scalafix"
    val args = CommandLineParser.tokenize(options)
    val emptySettings = new Settings(
      error => fail(s"couldn't apply settings because $error"))
    val reporter = new StoreReporter()
    val command = new CompilerCommand(args, emptySettings)
    val settings = command.settings
    val g = new Global(settings, reporter)
    val run = new g.Run
    g.phase = run.parserPhase
    g.globalPhase = run.parserPhase
    g
  }

  private object fixer extends NscSemanticApi {
    lazy val global: NscPluginSuite.this.g.type = NscPluginSuite.this.g
    def apply(unit: g.CompilationUnit): Fixed = fix(unit)
  }

  private def getParsedScalacTree(code: String): g.Tree = {
    import g._

    val run = g.currentRun
    g.phase = run.parserPhase
    g.globalPhase = run.parserPhase
    val reporter = new StoreReporter()
    g.reporter = reporter

    val tree = {
      if (parseAsCompilationUnit) {
        val cu = new CompilationUnit(newSourceFile(code))
        val parser = new syntaxAnalyzer.UnitParser(cu, Nil)
        parser.parse()
      } else {
        // NOTE: `parseStatsOrPackages` fails to parse abstract type defs without bounds,
        // so we need to apply a workaround to ensure that we correctly process those.
        def somewhatBrokenParse(code: String) =
          gen.mkTreeOrBlock(
            newUnitParser(code, "<toolbox>").parseStatsOrPackages())
        val rxAbstractTypeNobounds = """^type (\w+)(\[[^=]*?\])?$""".r
        code match {
          case rxAbstractTypeNobounds(_ *) =>
            val tdef @ TypeDef(mods, name, tparams, _) = somewhatBrokenParse(
              code + " <: Dummy")
            treeCopy.TypeDef(tdef,
                             mods,
                             name,
                             tparams,
                             TypeBoundsTree(EmptyTree, EmptyTree))
          case _ =>
            somewhatBrokenParse(code)
        }
      }
    }

    val errors = reporter.infos.filter(_.severity == g.reporter.ERROR)
    errors.foreach(error =>
      fail(s"scalac parse error: ${error.msg} at ${error.pos}"))
    tree
  }
  var packageCount = 0 // hack to isolate each test case in its own package namespace.
  def wrap(code: String): String = {
    val packageName = s"p$packageCount"
    val packagedCode = s"package $packageName { $code }"
    packagedCode
  }

  private def unwrap(gtree: g.Tree): g.Tree = gtree match {
    case g.PackageDef(g.Ident(g.TermName(_)), stat :: Nil) => stat
    case body => body
  }

  private def getTypedCompilationUnit(code: String): g.CompilationUnit = {
    import g._
    val packagedCode = wrap(code)
    val unit = new CompilationUnit(
      newSourceFile(packagedCode, "<ReflectToMeta>"))

    val run = g.currentRun
    val phases = List(run.parserPhase, run.namerPhase, run.typerPhase)
    val reporter = new StoreReporter()
    g.reporter = reporter

    phases.foreach(phase => {
      g.phase = phase
      g.globalPhase = phase
      phase.asInstanceOf[GlobalPhase].apply(unit)
      val errors = reporter.infos.filter(_.severity == reporter.ERROR)
      errors.foreach(error =>
        fail(s"scalac ${phase.name} error: ${error.msg} at ${error.pos}"))
    })
    unit
  }

  def fix(code: String): String = {
    val Fixed.Success(fixed) = fixer(getTypedCompilationUnit(code))
    fixed
  }

  def check(original: String, expected: String): Unit = {
    test(original) {
      val fixed = fix(original)
      assert(fixed == expected)
    }
  }
}

class ExplicitImplicitSuite extends NscPluginSuite(ExplicitImplicit) {
  check(
    """|class A {
       |  implicit val x = List(1)
       |}
    """.stripMargin,
    """|class A {
       |  implicit val x: List[Int] = List(1)
       |}
    """.stripMargin
  )
}

