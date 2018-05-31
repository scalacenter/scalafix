package scalafix.testkit

import java.nio.charset.StandardCharsets
import metaconfig.Conf
import metaconfig.Configured
import metaconfig.internal.ConfGet
import scala.meta.internal.io.FileIO
import scalafix.v1
import scala.meta._
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalafixConfig
import scalafix.v1.RuleDecoder

final class RuleTest(
    val filename: RelativePath,
    val run: () => Configured[(Rules, v1.SemanticDoc)]
)

object RuleTest {
  private val decoder = RuleDecoder.decoder()

  def fromDirectory(
      dir: AbsolutePath,
      classpath: Classpath,
      symtab: SymbolTable): Seq[RuleTest] = {
    FileIO.listAllFilesRecursively(dir).files.map { rel =>
      new RuleTest(
        rel, { () =>
          val input = Input.VirtualFile(
            rel.toString(),
            FileIO.slurp(dir.resolve(rel), StandardCharsets.UTF_8))
          val tree = input.parse[Source].get
          val doc = v1.Doc.fromTree(tree)
          val sdoc = v1.SemanticDoc.fromPath(doc, rel, classpath, symtab)
          val comment = SemanticRuleSuite.findTestkitComment(tree.tokens)
          val syntax = comment.syntax.stripPrefix("/*").stripSuffix("*/")
          val conf = Conf.parseString(rel.toString(), syntax).get
          val config = conf.as[ScalafixConfig]
          config.andThen(scalafixConfig => {
            val decoderSettings =
              RuleDecoder.Settings().withConfig(scalafixConfig)
            val decoder = RuleDecoder.decoder(decoderSettings)
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
