package scalafix.testkit

import java.nio.charset.StandardCharsets
import metaconfig.Conf
import metaconfig.Configured
import metaconfig.internal.ConfGet
import org.langmeta.internal.io.FileIO
import org.langmeta.internal.io.PathIO
import scalafix.v1._
import scala.meta._
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules
import scalafix.reflect.ScalafixReflectV1
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser

case class RuleTest(
    filename: RelativePath,
    run: () => Configured[(Rules, SemanticDoc)]
)
object RuleTest {
  val decoder = ScalafixReflectV1.decoder()

  def fromDirectory(
      dir: AbsolutePath,
      classpath: Classpath,
      symtab: SymbolTable): Seq[RuleTest] = {
    FileIO
      .listAllFilesRecursively(dir)
      .files
      .filter(p =>
        p.toNIO.startsWith("scala") &&
          PathIO.extension(p.toNIO) == "scala")
      .map { rel =>
        RuleTest(
          rel, { () =>
            val input = Input.VirtualFile(
              rel.toString(),
              FileIO.slurp(dir.resolve(rel), StandardCharsets.UTF_8))
            val tree = input.parse[Source].get
            val doc = Doc.fromTree(tree)
            val sdoc = SemanticDoc.fromPath(doc, rel, classpath, symtab)
            val comment = SemanticRuleSuite.findTestkitComment(tree.tokens)
            val syntax = comment.syntax.stripPrefix("/*").stripSuffix("*/")
            val decoder = ScalafixReflectV1.decoder()
            val conf = Conf.parseString(rel.toString(), syntax).get
            val rulesConf =
              ConfGet
                .getKey(conf, "rules" :: "rule" :: Nil)
                .getOrElse(Conf.Lst(Nil))
            decoder
              .read(rulesConf)
              .andThen(_.withConfig(conf))
              .map(_ -> sdoc)
          }
        )
      }
  }

}
