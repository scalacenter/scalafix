package scalafix

import org.scalatest.FunSuiteLike
import scala.collection.immutable.Seq
import scala.{meta => m}
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.{CompilerCommand, Global, Settings}
import scala.tools.nsc.reporters.StoreReporter

trait CompilerPluginSuite extends FunSuiteLike {
  val parseAsCompilationUnit = false

  private lazy val g: Global = {
    def fail(msg: String) =
      sys.error(s"ReflectToMeta initialization failed: $msg")
    val classpath = System.getProperty("sbt.paths.testsConverter.test.classes")
    val pluginpath = System.getProperty("sbt.paths.plugin.jar")
    val options = "-cp " + classpath + " -Xplugin:" + pluginpath + ":" + classpath + " -Xplugin-require:macroparadise"
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

  private def getTypedScalacTree(code: String): g.Tree = {
    unwrap(unwrap(getTypedCompilationUnit(code).body))
  }

}
