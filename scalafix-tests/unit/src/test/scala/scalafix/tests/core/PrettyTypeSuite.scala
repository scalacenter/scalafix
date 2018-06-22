package scalafix.tests.core

import scala.meta.internal.ScalametaInternals
import scala.meta.internal.io.PlatformFileIO
import scala.meta.internal.semanticdb.Index
import scala.meta.internal.trees.Origin
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scala.{meta => m}
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.LazySymbolTable
import scalafix.internal.util.QualifyStrategy
import scalafix.internal.util.PrettyType

class BasePrettyTypeSuite extends BaseSemanticSuite("TypeToTreeInput") {
  super.beforeAll()
  val dir: m.AbsolutePath =
    m.AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)
  val mclasspath = ClasspathOps
    .toMetaClasspath(
      m.Classpath(
        dir :: RuleCompiler.defaultClasspathPaths.filter { path =>
          path.isFile ||
          path.toNIO.getFileName.toString.contains("scala-library")
        }
      )
    )
    .get
  val table = new LazySymbolTable(mclasspath)
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
          case _: m.Defn.Object => "."
          case _: m.Pkg.Object => ".package."
          case _ => "#"
        }
        val info = table.info(s"test.$name$suffix").get
        val obtained =
          PrettyType
            .toTree(info, table, QualifyStrategy.Readable, fatalErrors = false)
            .tree
        val expectedSyntax =
          // TODO: Remove withOrigin after https://github.com/scalameta/scalameta/issues/1526
          ScalametaInternals.withOrigin(expected, Origin.None).syntax
        assertNoDiff(obtained.syntax, expectedSyntax)
      }
  }

}

class PrettyTypeFuzzSuite extends BasePrettyTypeSuite {

  for {
    entry <- mclasspath.entries
  } {
    // ignore these test  by default because they are slow and
    // there is no need to run them on every PR.
    ignore(entry.toNIO.getFileName.toString) {
      if (entry.isFile) {
        PlatformFileIO.withJarFileSystem(entry, create = false) { root =>
          val indexPath = root
            .resolve("META-INF")
            .resolve("semanticdb.semanticidx")
            .readAllBytes
          val index = Index.parseFrom(indexPath)
          index.toplevels.foreach { toplevel =>
            val info = table.info(toplevel.symbol).get
            try PrettyType.toTree(
              info,
              table,
              QualifyStrategy.Readable,
              fatalErrors = false
            )
            catch {
              case e: NoSuchElementException =>
                // Workaround for https://github.com/scalameta/scalameta/issues/1491
                // It's not clear how to fix that issue.
                cancel(e.getMessage)
              case NonFatal(e) =>
                throw new IllegalArgumentException(info.toProtoString, e)
                with NoStackTrace
            }
          }
        }
      }

    }
  }
}
