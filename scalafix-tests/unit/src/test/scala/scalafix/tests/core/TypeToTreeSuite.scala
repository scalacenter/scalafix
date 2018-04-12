package scalafix.tests.core

import org.langmeta.internal.io.FileIO
import org.scalameta.logger
import org.scalatest.FunSuite
import scala.{meta => m}
import scala.meta.internal.{semanticdb3 => s}
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.reflect.RuleCompiler
import scalafix.internal.util.Shorten
import scalafix.internal.util.TypeToTree
import scalafix.testkit.DiffAssertions

class TypeToTreeSuite extends BaseSemanticTest("TypeToTreeInput") {
  test("synthesized tree matches original trait") {
//    logger.elem(index.database)
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
    val pretty = new TypeToTree(table, Shorten.Readable)
    pprint.log(table.info("test.TypeToTreeInput#f().").get.toProtoString)
    val info = table.info("test.TypeToTreeInput#").get
    val expected  = source.collect { case d: m.Defn.Trait => d }.head
    val obtained = pretty.toTree(info).tree
    assertNoDiff(obtained.syntax, expected.syntax)
  }

}
