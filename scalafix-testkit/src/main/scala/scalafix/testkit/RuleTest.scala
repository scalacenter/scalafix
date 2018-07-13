package scalafix.testkit

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
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
  private val isScalaFile =
    FileSystems.getDefault.getPathMatcher("glob:**.scala")

  def fromDirectory(
      offset: RelativePath,
      dir: AbsolutePath,
      classLoader: ClassLoader,
      symtab: SymbolTable): Seq[RuleTest] = {
    val scalaFiles =
      FileIO.listAllFilesRecursively(dir).files.filter(isScalaFile.matches(_))
    scalaFiles.map { rel =>
      new RuleTest(
        rel, { () =>
          val input = Input.VirtualFile(
            rel.toString(),
            FileIO.slurp(dir.resolve(rel), StandardCharsets.UTF_8))
          val tree = input.parse[Source].get
          val doc = v1.Doc.fromTree(tree)
          val sdoc = v1.SemanticDoc
            .fromPath(doc, offset.resolve(rel), classLoader, symtab)
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
