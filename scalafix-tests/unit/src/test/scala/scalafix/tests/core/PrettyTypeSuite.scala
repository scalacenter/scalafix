package scalafix.tests.core

import scala.meta.internal.ScalametaInternals
import scala.meta.internal.io.PathIO
import scala.meta.internal.io.PlatformFileIO
import scala.meta.internal.trees.Origin
import scala.meta.internal.symtab._
import scala.meta.internal.metacp._
import scala.meta.internal.classpath.ClasspathIndex
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scala.{meta => m}
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.io.AbsolutePath
import scalafix.internal.util.QualifyStrategy
import scalafix.internal.util.PrettyType

class BasePrettyTypeSuite extends BaseSemanticSuite("TypeToTreeInput") {
  super.beforeAll()
  val dir: m.AbsolutePath =
    m.AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)
  val classpath = Classpaths.withDirectory(dir)
  val table = GlobalSymbolTable(classpath)
}

class PrettyTypeSuite extends BasePrettyTypeSuite {

  val m.Source(m.Pkg(_, stats) :: Nil) = source.transform {
    // Remove bodies from methods like `private def foo: Unit` that can't be abstract.
    case m.Defn.Def(mods, name, tparams, paramss, Some(decltpe), _) =>
      m.Decl.Def(mods, name, tparams, paramss, decltpe)
  }
  stats.collect {
    case expected: m.Member =>
      val name = expected.name.value
      test(s"${expected.productPrefix} - $name") {
        val suffix: String = expected match {
          case _: m.Defn.Object => s"$name."
          case _: m.Pkg.Object => s"$name/package."
          case _ => s"$name#"
        }
        val sym = s"test/$suffix"
        val info =
          table.info(sym).getOrElse(throw new NoSuchElementException(sym))
        val obtained =
          PrettyType
            .toTree(info, table, QualifyStrategy.Readable, fatalErrors = true)
            .tree
        val expectedSyntax =
          // TODO: Remove withOrigin after https://github.com/scalameta/scalameta/issues/1526
          ScalametaInternals.withOrigin(expected, Origin.None).syntax
        assertNoDiff(obtained.syntax, expectedSyntax)
      }
  }

}

class PrettyTypeFuzzSuite extends BasePrettyTypeSuite {

  val classpathIndex = ClasspathIndex(classpath)

  def checkToplevel(toplevel: SymbolInformation): Unit = {
    try {
      val info = table.info(toplevel.symbol).getOrElse {
        throw new NoSuchElementException(toplevel.symbol)
      }
      PrettyType.toTree(
        info,
        table,
        QualifyStrategy.Readable,
        fatalErrors = false
      )
    } catch {
      case e: NoSuchElementException
          // FIXME
          if e.getMessage.endsWith("_#") ||
            // contains weird macro-generated symbols like MacroTreeBuilder#`_956.type`#
            e.getMessage.contains("MacroTreeBuilder") ||
            e.getMessage.contains("javax/servlet/http/HttpServlet#") ||
            e.getMessage.contains("javax/servlet/ServletContextListener#") ||
            e.getMessage.endsWith("`?0`#") ||
            e.getMessage.endsWith("`1`#") ||
            e.getMessage.endsWith("`2`#") =>
        ()
      case NonFatal(e) =>
        throw new IllegalArgumentException(toplevel.toProtoString, e)
        with NoStackTrace
    }
  }

  def checkPath(file: AbsolutePath): Unit = {
    val node = file.toClassNode
    ClassfileInfos
      .fromClassNode(node, classpathIndex)
      .foreach { classfile =>
        classfile.infos.foreach { toplevel =>
          if (toplevel.symbol.owner.isPackage) {
            checkToplevel(toplevel)
          }
        }
      }
  }

  def ignore(path: AbsolutePath): Boolean = {
    val uri = path.toURI.toString
    uri.contains("package-info") ||
    uri.contains("scala/tools/nsc/interpreter/jline") ||
    uri.contains("scala/tools/ant/") ||
    uri.contains("org/scalatest/tools/HtmlReporter") ||
    uri.contains("org/scalatest/tools/ScalaTestAntTask") ||
    uri.contains("org/scalatest/testng") ||
    uri.contains("org/scalatest/selenium") ||
    uri.contains("org/scalatest/mockito") ||
    uri.contains("org/scalatest/easymock") ||
    uri.contains("org/scalatest/jmock") ||
    uri.contains("org/scalatest/junit")
  }

  for {
    entry <- classpath.entries
    if entry.isFile
  } {
    // ignore these test  by default because they are slow and
    // there is no need to run them on every PR.
    ignore(entry.toNIO.getFileName.toString) {
      PlatformFileIO.withJarFileSystem(entry, create = false) { root =>
        val files = PlatformFileIO.listAllFilesRecursively(root)
        files.foreach { file =>
          if (PathIO.extension(file.toNIO) == "class" && !ignore(file)) {
            try {
              checkPath(file)
            } catch {
              case scala.meta.internal.classpath.MissingSymbolException(e) =>
                pprint.log(file)
            }
          }
        }
      }
    }
  }
}
