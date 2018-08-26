package scalafix.testkit

import metaconfig.Conf
import metaconfig.internal.ConfGet
import scalafix.v1
import scala.meta._
import scala.meta.internal.symtab.SymbolTable
import scalafix.internal.v1.Rules
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.v1.LazyValue
import scalafix.v1.Configuration
import scalafix.v1.RuleDecoder

final class RuleTest(
    val path: TestkitPath,
    val run: () => (Rules, v1.SemanticDoc)
)

object RuleTest {
  private[scalafix] def fromPath(
      props: TestkitProperties,
      test: TestkitPath,
      classLoader: ClassLoader,
      symtab: SymbolTable): RuleTest = {
    val run: () => (Rules, v1.SemanticDoc) = { () =>
      val input = test.toInput
      val tree = input.parse[Source].get
      val comment = SemanticRuleSuite.findTestkitComment(tree.tokens)
      val syntax = comment.syntax.stripPrefix("/*").stripSuffix("*/")
      val conf = Conf.parseString(test.testName, syntax).get
      val scalafixConfig = conf.as[ScalafixConfig].get
      val doc = v1.Doc(
        tree.pos.input,
        LazyValue.now(tree),
        DiffDisable.empty,
        scalafixConfig
      )
      val sdoc =
        v1.SemanticDoc.fromPath(doc, test.semanticdbPath, classLoader, symtab)
      val decoderSettings =
        RuleDecoder.Settings().withConfig(scalafixConfig)
      val decoder = RuleDecoder.decoder(decoderSettings)
      val rulesConf = ConfGet
        .getKey(conf, "rules" :: "rule" :: Nil)
        .getOrElse(Conf.Lst(Nil))
      val config = Configuration()
        .withConf(conf)
        .withScalaVersion(props.scalaVersion)
        .withScalacOptions(props.scalacOptions)
      val rules = decoder.read(rulesConf).get.withConfiguration(config).get
      (rules, sdoc)
    }

    new RuleTest(test, run)
  }
}
