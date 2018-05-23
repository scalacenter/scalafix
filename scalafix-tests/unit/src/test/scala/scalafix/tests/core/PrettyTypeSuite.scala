package scalafix.tests.core

import org.langmeta.internal.io.PlatformFileIO
import org.scalatest.Ignore
import scala.meta.internal.semanticdb3.Index
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import scala.{meta => m}
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.LazySymbolTable
import scalafix.internal.util.QualifyStrategy
import scalafix.internal.util.PrettyType

class BasePrettyTypeSuite extends BaseSemanticTest("TypeToTreeInput") {
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

  val m.Source(m.Pkg(_, stats) :: Nil) = source
  stats.collect {
    case _: m.Defn.Object => // blocked by https://github.com/scalameta/scalameta/issues/1480
    case expected: m.Member =>
      val name = expected.name.value
      test(s"${expected.productPrefix} - $name") {
        val info = table.info(s"test.$name#").get
        val obtained =
          PrettyType.toTree(info, table, QualifyStrategy.Readable).tree
        assertNoDiff(obtained.syntax, expected.syntax)
      }
  }

}

// This test is slow, no need to run it on every PR
@Ignore
class PrettyTypeFuzzSuite extends BasePrettyTypeSuite {

  for {
    entry <- mclasspath.shallow
  } {
    test(entry.toNIO.getFileName.toString) {
      if (entry.isFile) {
        PlatformFileIO.withJarFileSystem(entry, create = false) { root =>
          val indexPath = root
            .resolve("META-INF")
            .resolve("semanticdb.semanticidx")
            .readAllBytes
          val index = Index.parseFrom(indexPath)
          index.toplevels.foreach { toplevel =>
            val info = table.info(toplevel.symbol).get
            try PrettyType.toTree(info, table, QualifyStrategy.Readable)
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
