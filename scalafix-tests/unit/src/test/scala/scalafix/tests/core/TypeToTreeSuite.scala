package scalafix.tests.core

import scala.{meta => m}
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.Shorten
import scalafix.internal.util.TypeToTree

class TypeToTreeSuite extends BaseSemanticTest("TypeToTreeInput") {
  super.beforeAll()
  val dir: m.AbsolutePath =
    m.AbsolutePath(scalafix.tests.BuildInfo.sharedClasspath)
  val table = ClasspathOps
    .newSymbolTable(
      m.Classpath(
        dir :: RuleCompiler.defaultClasspathPaths.filter { path =>
          path.toNIO.getFileName.toString.contains("scala-library")
        }
      )
    )
    .get
  val m.Source(m.Pkg(_, stats) :: Nil) = source
  stats.collect {
    case _: m.Defn.Object => // blocked by https://github.com/scalameta/scalameta/issues/1480
    case expected: m.Member =>
      val name = expected.name.value
      test(s"${expected.productPrefix} - $name") {
        val pretty = new TypeToTree(table, Shorten.Readable)
        val info = table.info(s"test.$name#").get
        val obtained = pretty.toTree(info).tree
        assertNoDiff(obtained.syntax, expected.syntax)
      }
  }

}
