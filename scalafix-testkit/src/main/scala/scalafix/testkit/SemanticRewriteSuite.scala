package scalafix.testkit

import scala.collection.immutable.Seq
import scala.meta.Term
import scala.meta.Tree
import scala.meta.contrib._
import scala.meta.internal.scalahost.ScalahostPlugin
import scala.meta.internal.scalahost.v1.online.Mirror
import scala.meta.semantic.v1.Database
import scala.reflect.io.AbstractFile
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter
import scala.util.control.NonFatal
import scala.{meta => m}
import scalafix.Fixed
import scalafix.syntax._
import scalafix.config.ScalafixConfig
import scalafix.nsc.ScalafixNscPlugin
import scalafix.rewrite.ScalafixMirror

import java.io.File
import java.io.PrintWriter
import java.net.URLClassLoader

import org.scalameta.logger
import org.scalatest.FunSuite

abstract class SemanticRewriteSuite(classpath: String)
    extends FunSuite
    with DiffAssertions { self =>
  private val testGlobal: Global = {
    def fail(msg: String) =
      sys.error(s"ReflectToMeta initialization failed: $msg")

    val scalacOptions = Seq(
      "-cp",
      classpath,
      "-Yrangepos",
      "-Ywarn-unused-import"
    ).mkString(" ", " ", " ")

    val args = CommandLineParser.tokenize(scalacOptions)
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

  private val scalafixNscPlugin = new ScalafixNscPlugin(testGlobal)
  private val fixer = scalafixNscPlugin.component
  private val g: fixer.global.type = fixer.global
  implicit val mirror = scalafixNscPlugin.mirror

  import mirror._

  private def unwrap(gtree: g.Tree): g.Tree = gtree match {
    case g.PackageDef(g.Ident(g.TermName(_)), stat :: Nil) => stat
    case body => body
  }

  private def computeDatabaseFromSnippet(
      code: String): (g.CompilationUnit, Database) = {
    val javaFile = File.createTempFile("paradise", ".scala")
    val writer = new PrintWriter(javaFile)
    try writer.write(code)
    finally writer.close()

    val run = new g.Run
    val abstractFile = AbstractFile.getFile(javaFile)
    val sourceFile = g.getSourceFile(abstractFile)
    val unit = new g.CompilationUnit(sourceFile)
    run.compileUnits(List(unit), run.phaseNamed("terminal"))

    g.phase = run.parserPhase
    g.globalPhase = run.parserPhase
    val reporter = new StoreReporter()
    g.reporter = reporter
    unit.body = g.newUnitParser(unit).parse()
    val errors = reporter.infos.filter(_.severity == reporter.ERROR)
    errors.foreach(error =>
      fail(s"scalac parse error: ${error.msg} at ${error.pos}"))

    val packageobjectsPhase = run.phaseNamed("packageobjects")
    val phases = List(run.parserPhase,
                      run.namerPhase,
                      packageobjectsPhase,
                      run.typerPhase)
    reporter.reset()

    phases.foreach(phase => {
      g.phase = phase
      g.globalPhase = phase
      phase.asInstanceOf[g.GlobalPhase].apply(unit)
      val errors = reporter.infos.filter(_.severity == reporter.ERROR)
      errors.foreach(error =>
        fail(s"scalac ${phase.name} error: ${error.msg} at ${error.pos}"))
    })
    g.phase = run.phaseNamed("patmat")
    g.globalPhase = run.phaseNamed("patmat")

    unit -> unit.asInstanceOf[mirror.g.CompilationUnit].toDatabase
  }

  def fix(code: String,
          config: Option[ScalafixMirror] => ScalafixConfig): String = {
    val (unit, database) = computeDatabaseFromSnippet(code)
    val fixed = fixer.fix(unit, config(None), m => config(Some(m)).rewrite).get
    fixed
  }

  case class MismatchException(details: String) extends Exception

  private def checkMismatchesModuloDesugarings(obtained: m.Tree,
                                               expected: m.Tree): Unit = {
    import scala.meta._
    def loop(x: Any, y: Any): Boolean = {
      val ok = (x, y) match {
        case (x, y) if x == null || y == null =>
          x == null && y == null
        case (x: Some[_], y: Some[_]) =>
          loop(x.get, y.get)
        case (x: None.type, y: None.type) =>
          true
        case (xs: Seq[_], ys: Seq[_]) =>
          xs.length == ys.length && xs.zip(ys).forall {
            case (x, y) => loop(x, y)
          }
        case (x: Tree, y: Tree) =>
          def sameStructure =
            x.productPrefix == y.productPrefix &&
              loop(x.productIterator.toList, y.productIterator.toList)

          sameStructure
        case _ =>
          x == y
      }
      if (!ok) {
        val structure = (x, y) match {
          case (t1: Tree, t2: Tree) =>
            s"""
               |Diff:
               |${t1.structure}
               |${t2.structure}
               |""".stripMargin
          case _ => ""
        }
        throw MismatchException(s"$x != $y$structure")
      } else true
    }

    loop(obtained, expected)
  }

  private def typeChecks(code: String): Unit = {
    try {
      computeDatabaseFromSnippet(code)
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        fail(
          s"""Fixed source code does not typecheck!
             |Message: ${e.getMessage}
             |Reveal: ${code.revealWhiteSpace}
             |Code: $code""".stripMargin,
          e
        )
    }
  }

  private def parse(code: String): m.Tree = {
    import scala.meta._
    code.parse[Source].get
  }

  def assertNoDiff(obtained: Tree, expected: Tree): Boolean = {
    assertNoDiff(obtained.tokens.mkString,
                 expected.tokens.mkString,
                 "Tree syntax mismatch")
  }

  def check(original: String, expectedStr: String, diffTest: DiffTest): Unit = {
    def formatHeader(header: String): String = {
      val line = s"=" * (header.length + 3)
      s"$line\n=> $header\n$line"
    }
    val fixed = fix(diffTest.wrapped(), diffTest.config)
    val obtained = parse(diffTest.unwrap(fixed))
    val expected = parse(expectedStr)
    try {
      typeChecks(diffTest.wrapped(fixed))
      checkMismatchesModuloDesugarings(obtained, expected)
      if (diffTest.checkSyntax) {
        assertNoDiff(obtained, expected)
      } else {
        checkMismatchesModuloDesugarings(obtained, expected)
      }
    } catch {
      case MismatchException(details) =>
        val header = s"scala -> meta converter error\n$details"
        val fullDetails =
          s"""${formatHeader("Expected")}
             |${expected.syntax}
             |${formatHeader("Obtained")}
             |${obtained.syntax}""".stripMargin
        fail(s"$header\n$fullDetails")
    }
  }

  def runDiffTest(dt: DiffTest): Unit = {
    if (dt.skip) {
      ignore(dt.fullName) {}
    } else {
      test(dt.fullName) {
        check(dt.original, dt.expected, dt)
      }
    }
  }
}
object SemanticRewriteSuite {
  def thisClasspath: String = this.getClass.getClassLoader match {
    case u: URLClassLoader =>
      u.getURLs.map(_.getPath).mkString(java.io.File.pathSeparator)
    case _ => ""
  }
}
