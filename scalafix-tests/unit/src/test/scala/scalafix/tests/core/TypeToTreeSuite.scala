package scalafix.tests.core

import org.langmeta.internal.io.PlatformFileIO
import org.scalatest.exceptions.TestFailedException
import scala.meta.internal.semanticdb3.Index
import scala.util.control.NonFatal
import scala.{meta => m}
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.LazySymbolTable
import scalafix.internal.util.Shorten
import scalafix.internal.util.TypeToTree

class BaseTypeToTreeSuite extends BaseSemanticTest("TypeToTreeInput") {
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
  val pretty = new TypeToTree(table, Shorten.Readable)
}

class TypeToTreeSuite extends BaseTypeToTreeSuite {

  val m.Source(m.Pkg(_, stats) :: Nil) = source
  stats.collect {
    case _: m.Defn.Object => // blocked by https://github.com/scalameta/scalameta/issues/1480
    case expected: m.Member =>
      val name = expected.name.value
      test(s"${expected.productPrefix} - $name") {
        val info = table.info(s"test.$name#").get
        val obtained = pretty.toTree(info).tree
        assertNoDiff(obtained.syntax, expected.syntax)
      }
  }

}
class TypeToTreeFuzzSuite extends BaseTypeToTreeSuite {
  // Workaround for https://github.com/scalameta/scalameta/issues/1491
  val isKnownBug = Set(
    "java.util.jar.Attributes.Name#",
    "java.lang.Thread.UncaughtExceptionHandler#",
    "java.lang.invoke.MethodHandles.Lookup#",
    "java.util.concurrent.ForkJoinPool."
  )

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
            scala.tools.nsc.interpreter.StdReplTags
            try pretty.toTree(info).tree
            catch {
              case e: NoSuchElementException if isKnownBug(e.getMessage) =>
              case e: NoSuchElementException =>
                fail(e.getMessage)
              case NonFatal(e) =>
                fail(info.toProtoString, e)
            }
          }
        }
      }

    }
  }
}
