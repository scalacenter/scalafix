package scalafix.testkit

import scala.meta._
import scala.meta.internal.symtab.SymbolTable

import scalafix.internal.diff.DiffDisable
import scalafix.internal.v1.Args
import scalafix.internal.v1.LazyValue
import scalafix.internal.v1.Rules
import scalafix.v1
import scalafix.v1.Configuration
import scalafix.v1.RuleDecoder

final class RuleTest(
    val path: TestkitPath,
    val run: () => (Rules, v1.SemanticDocument)
)

object RuleTest {
  private[scalafix] def fromPath(
      args: Args,
      test: TestkitPath,
      classLoader: ClassLoader,
      symtab: SymbolTable
  ): RuleTest = {
    val run: () => (Rules, v1.SemanticDocument) = { () =>
      val input = test.toInput
      val dialect = args.scalaVersion.dialect(args.sourceScalaVersion)
      val tree = dialect(input).parse[Source].get
      val (_, conf, rulesConf, scalafixConfig) =
        SemanticRuleSuite.parseTestkitComment(tree.tokens)
      val doc = v1.SyntacticDocument(
        tree.pos.input,
        LazyValue.now(tree),
        DiffDisable.empty,
        scalafixConfig
      )
      val sdoc =
        v1.SemanticDocument.fromPath(
          doc,
          test.semanticdbPath,
          classLoader,
          symtab
        )
      val decoderSettings =
        RuleDecoder.Settings().withConfig(scalafixConfig)
      val decoder = RuleDecoder.decoder(decoderSettings)
      val config = Configuration()
        .withConf(conf)
        .withScalaVersion(args.scalaVersion.value)
        .withScalacOptions(args.scalacOptions)
        .withScalacClasspath(args.classpath.entries)
      val rules = decoder.read(rulesConf).get.withConfiguration(config).get
      (rules, sdoc)
    }

    new RuleTest(test, run)
  }
}
