package scalafix.tests.core

import org.langmeta.internal.io.PlatformFileIO
import scala.meta.internal.semanticdb3.Index
import scala.{meta => m}
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.LazySymbolTable
import scalafix.internal.util.Shorten
import scalafix.internal.util.TypeToTree

class TypeToTreeSuite extends BaseSemanticTest("TypeToTreeInput") {
  super.beforeAll()
  val dir: m.AbsolutePath =
    m.AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)
  val mclasspath = ClasspathOps
    .toMetaClasspath(
      m.Classpath(
        dir :: RuleCompiler.defaultClasspathPaths.filter { path =>
          path.toNIO.getFileName.toString.contains("scala-library")
        }
      )
    )
    .get
  val table = new LazySymbolTable(mclasspath)
  val pretty = new TypeToTree(table, Shorten.Readable)
//  val m.Source(m.Pkg(_, stats) :: Nil) = source
//  stats.collect {
//    case _: m.Defn.Object => // blocked by https://github.com/scalameta/scalameta/issues/1480
//    case expected: m.Member =>
//      val name = expected.name.value
//      ignore(s"${expected.productPrefix} - $name") {
//        val info = table.info(s"test.$name#").get
//        val obtained = pretty.toTree(info).tree
//        assertNoDiff(obtained.syntax, expected.syntax)
//      }
//  }

  for {
    entry <- mclasspath.shallow
    if entry.toString().contains("rt-")
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
            val obtained = pretty.toTree(info).tree
          }
        }
      }

    }
  }
}
