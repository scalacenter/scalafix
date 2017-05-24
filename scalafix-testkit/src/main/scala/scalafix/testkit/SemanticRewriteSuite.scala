package scalafix
package testkit

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.internal.semantic.DatabaseOps
import scala.reflect.io.AbstractFile
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter
import scala.util.control.NonFatal
import scala.{meta => m}
import scalafix.config.ScalafixConfig
import scalafix.rewrite.RewriteCtx
import scalafix.syntax._

import java.io.File
import java.io.PrintWriter
import java.net.URL
import java.net.URLClassLoader

import metaconfig.ConfError
import org.scalatest.FunSuite

/**
  *
  * @param classpath the classpath to use to compile rewrite unit tests.
  *                  Defaults to the classpath of the current classloader.
  * @param scalahostPluginPath the file path to the scalahost compiler plugin,
  *                            which is passed as `-Xplugin:/path/scalahost.jar`
  *                            to scalac in order to build a semantic DB
  *                            when compiling .source unit test files.
  */
abstract class SemanticRewriteSuite(
    classpath: String = SemanticRewriteSuite.thisClasspath,
    scalahostPluginPath: File = SemanticRewriteSuite.scalahostJarPath
) extends FunSuite
    with DiffAssertions { self =>
  val g: Global = {
    def fail(msg: String) =
      sys.error(s"ReflectToMeta initialization failed: $msg")
    // TODO(olafur) hack, find a way to pass this path via buildinfo
    val scalacOptions = Seq(
      "-Yrangepos",
      "-cp",
      classpath,
      "-Xplugin:" + scalahostPluginPath,
      "-Xplugin-require:scalahost",
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

  lazy val databaseOps: DatabaseOps { val global: self.g.type } =
    new DatabaseOps { override val global: self.g.type = self.g }
  import databaseOps._

  def runDiffTest(dt: DiffTest): Unit = {
    if (dt.skip) {
      ignore(dt.fullName) {}
    } else {
      test(dt.fullName) {
        check(dt.original, dt.expected, dt)
      }
    }
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
      assertNoDiff(obtained, expected)
      typeChecks(diffTest.wrapped(fixed))
      checkMismatchesModuloDesugarings(obtained, expected)
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

  def fix(code: String,
          getConfig: Option[Mirror] => (Rewrite, ScalafixConfig)): String = {
    val mirror = computeMirror(code)
    val (rewrite, config) = getConfig(Some(mirror))
    val tree = mirror.sources.head
    val ctx = RewriteCtx(tree, config)
    rewrite.apply(ctx)
  }

  def withCwd[T](f: File => T): T = {
    val javaFile = File.createTempFile("paradise", ".scala")
    databaseOps.config.setSourceroot(AbsolutePath(javaFile.getParentFile))
    f(javaFile)
  }

  private def computeMirror(code: String): Mirror = withCwd { javaFile =>
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
      errors.foreach {
        case reporter.Info(pos, msg, reporter.ERROR) =>
          val formattedMessage =
            ConfError
              .msg(msg)
              .atPos(
                m.Position.Range(Input.File(javaFile), pos.start, pos.end))
              .notOk
              .toString
          fail(formattedMessage)
        case _ => // nothing
      }
    })
    g.phase = run.phaseNamed("patmat")
    g.globalPhase = run.phaseNamed("patmat")
    new Mirror {
      override def database =
        Database(
          List(
            unit.source.toInput -> unit.toAttributes
          ))
    }
  }

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
      computeMirror(code)
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

  case class MismatchException(details: String) extends Exception
}

object SemanticRewriteSuite {
  def thisClasspathLst: List[URL] = this.getClass.getClassLoader match {
    case u: URLClassLoader => u.getURLs.toList
    case _ => Nil
  }
  def thisClasspath: String =
    thisClasspathLst.mkString(java.io.File.pathSeparator)

  def scalahostJarPath: File =
    new File(thisClasspathLst.find(_.toString.contains("scalahost")).get.toURI)
}
