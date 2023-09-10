package scalafix.testkit

import scala.util.Try

import scala.meta._
import scala.meta.internal.symtab.SymbolTable

import metaconfig.Conf
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scalafix.internal.config.ScalafixConfig
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
      // Its possible to have comments which isn't valid HOCON (i.e.
      // license headers), so lets parse until we find valid HOCON.
      val allComments =
        SemanticRuleSuite.filterPossibleTestkitComments(tree.tokens)
      val lazilyParsedComments = allComments.view.map { comment =>
        val syntax = comment.syntax.stripPrefix("/*").stripSuffix("*/")
        for {
          conf <- Try(Conf.parseString(test.testName, syntax)).toEither
            .flatMap(_.toEither)
          scalafixConfig <- conf.as[ScalafixConfig].toEither
        } yield {
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
          (RuleDecoder.decoder(decoderSettings), conf, sdoc)
        }
      }

      lazilyParsedComments
        .collectFirst { case Right((decoder, conf, sdoc)) =>
          val rulesConf = ConfGet
            .getKey(conf, "rules" :: "rule" :: Nil)
            .getOrElse(Conf.Lst(Nil))
          val config = Configuration()
            .withConf(conf)
            .withScalaVersion(args.scalaVersion.value)
            .withScalacOptions(args.scalacOptions)
            .withScalacClasspath(args.classpath.entries)
          val rules = decoder.read(rulesConf).get.withConfiguration(config).get
          (rules, sdoc)
        }
        .getOrElse(
          throw new IllegalArgumentException(
            s"Expected a single comment at ${test.testPath} with a valid rule or rules key defined"
          )
        )
    }

    new RuleTest(test, run)
  }
}
