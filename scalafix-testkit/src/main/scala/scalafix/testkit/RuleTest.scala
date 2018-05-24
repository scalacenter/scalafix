package scalafix.testkit

import java.nio.charset.StandardCharsets
import metaconfig.Conf
import metaconfig.Configured
import metaconfig.internal.ConfGet
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scalafix.v1._
import scala.meta._
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules
import scalafix.reflect.ScalafixReflectV1
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.util.ClassloadRule

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
            val conf = Conf.parseString(rel.toString(), syntax).get
            val config = conf.as[ScalafixConfig]
            config.andThen(scalafixConfig => {
              val decoder = ScalafixReflectV1
                .decoder(scalafixConfig, ClassloadRule.defaultClassloader)
              val rulesConf =
                ConfGet
                  .getKey(conf, "rules" :: "rule" :: Nil)
                  .getOrElse(Conf.Lst(Nil))
              decoder
                .read(rulesConf)
                .andThen(_.withConfig(conf))
                .map(_ -> sdoc)
            })
          }
        )
      }
  }

}
