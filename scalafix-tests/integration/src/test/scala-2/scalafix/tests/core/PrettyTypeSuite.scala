package scalafix.tests.core

import scala.collection.mutable
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scala.{meta => m}

import scala.meta.cli.Reporter
import scala.meta.internal.classpath.ClasspathIndex
import scala.meta.internal.io.PathIO
import scala.meta.internal.io.PlatformFileIO
import scala.meta.internal.metacp._
import scala.meta.internal.semanticdb.Scala._
import scala.meta.internal.semanticdb.SymbolInformation
import scala.meta.internal.symtab._
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.meta.metacp.Settings

import scalafix.internal.util.PrettyType
import scalafix.internal.util.QualifyStrategy

class BasePrettyTypeSuite extends BaseSemanticSuite("TypeToTreeInput") {
  super.beforeAll()
  val classDir: m.AbsolutePath =
    m.AbsolutePath(scalafix.tests.BuildInfo.classDirectory)
  val semanticdbTargetRoots: List[AbsolutePath] =
    scalafix.tests.BuildInfo.semanticClasspath
      .map(m.AbsolutePath.apply)
      .toList

  val classpath: Classpath =
    Classpaths.withDirectories(semanticdbTargetRoots :+ classDir)

  // As of scalameta 4.5.3, this relies on scalap (and not on TASTy), so it
  // cannot work against classes compiled with Scala 3
  val table: GlobalSymbolTable = GlobalSymbolTable(classpath, includeJdk = true)
}

class PrettyTypeSuite extends BasePrettyTypeSuite {

  val m.Source(m.Pkg(_, stats) :: Nil) = source.transform {
    // Remove bodies from methods like `private def foo: Unit` that can't be abstract.
    case m.Defn.Def(mods, name, tparams, paramss, Some(decltpe), _) =>
      m.Decl.Def(mods, name, tparams, paramss, decltpe)
  }

  // ignoring Functor[C[_]] because of a regression with scalac 2.13.7, see https://github.com/scalacenter/scalafix/pull/1493
  val filteredStats: List[m.Stat with m.Member] = stats.collect {
    case m: m.Member if m.name.value != "Functor" => m
  }

  filteredStats.collect { case expected: m.Member =>
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
      val expectedSyntax = expected.syntax
      assertNoDiff(obtained.syntax, expectedSyntax)
    }
  }

}

class PrettyTypeFuzzSuite extends BasePrettyTypeSuite {

  val classpathIndex: ClasspathIndex =
    ClasspathIndex(classpath, includeJdk = true)

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
    val settings = Settings()
    val reporter = Reporter().withSilentOut().withSilentErr()
    val node = file.toClassNode
    ClassfileInfos
      .fromClassNode(node, classpathIndex, settings, reporter)
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

  val isUnique: mutable.Set[String] = scala.collection.mutable.Set.empty[String]
  for {
    entry <- classpath.entries
    if entry.isFile
    name = entry.toNIO.getFileName.toString
    if isUnique(name)
  } {
    isUnique += name
    // ignore these test  by default because they are slow and
    // there is no need to run them on every PR.
    ignore(name) {
      PlatformFileIO.withJarFileSystem(entry, create = false) { root =>
        val files = PlatformFileIO.listAllFilesRecursively(root)
        files.foreach { file =>
          if (PathIO.extension(file.toNIO) == "class" && !ignore(file)) {
            try {
              checkPath(file)
            } catch {
              case scala.meta.internal.classpath.MissingSymbolException(e) =>
                println(file)
            }
          }
        }
      }
    }
  }
}
