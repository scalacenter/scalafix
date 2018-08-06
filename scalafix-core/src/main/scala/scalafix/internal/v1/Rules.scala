package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scala.meta.tokens.Tokens
import scalafix.internal.config.DisableConfig
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.config.NoInferConfig
import scalafix.internal.rule._
import scalafix.internal.util.SuppressOps
import scalafix.lint.LintDiagnostic
import scalafix.lint.LintMessage
import scalafix.patch.Patch
import scalafix.rule.RuleName
import scalafix.rule.ScalafixRules
import scalafix.v0
import scalafix.v1.Doc
import scalafix.v1.Rule
import scalafix.v1.SemanticDoc
import scalafix.v1.SemanticRule
import scalafix.v1.SyntacticRule

case class Rules(rules: List[Rule] = Nil) {

  def name: RuleName = RuleName(rules.flatMap(_.name.identifiers))
  def isEmpty: Boolean = rules.isEmpty
  def isSemantic: Boolean = semanticRules.nonEmpty
  def withConfig(conf: Conf): Configured[Rules] =
    MetaconfigPendingUpstream
      .traverse(rules.map(_.withConfig(conf)))
      .map(Rules(_))
  def semanticRules: List[SemanticRule] = rules.collect {
    case s: SemanticRule => s
  }
  def syntacticRules: List[SyntacticRule] = rules.collect {
    case s: SyntacticRule => s
  }

  def addSuppression(
      tokens: Tokens,
      messages: List[LintMessage],
      patch: Patch,
      suppress: Boolean): Patch = {
    if (suppress) {
      patch + SuppressOps.addComments(tokens, messages.map(_.position))
    } else {
      patch
    }
  }

  def semanticPatch(
      doc: SemanticDoc,
      suppress: Boolean): (String, List[LintDiagnostic]) = {
    val fixes = rules.iterator.map {
      case rule: SemanticRule =>
        rule.name -> rule.fix(doc)
      case rule: SyntacticRule =>
        rule.name -> rule.fix(doc.doc)
    }.toMap
    scalafix.Patch.apply(
      fixes,
      doc.doc.toRuleCtx,
      Some(doc.toSemanticdbIndex),
      suppress)
  }

  def syntacticPatch(
      doc: Doc,
      suppress: Boolean): (String, List[LintDiagnostic]) = {
    require(!isSemantic, semanticRules.map(_.name).mkString("+"))
    val fixes = syntacticRules.iterator.map { rule =>
      rule.name -> rule.fix(doc)
    }.toMap
    scalafix.Patch.apply(fixes, doc.toRuleCtx, None, suppress)
  }
}

object Rules {
  def defaults: List[Rule] = legacySemanticRules ++ legacySyntacticRules

  val legacySyntacticRules: List[LegacySyntacticRule] = {
    ScalafixRules.syntax.map(rule => new LegacySyntacticRule(rule))
  }

  val legacySemanticRules: List[LegacySemanticRule] = {
    val semantics = List[v0.SemanticdbIndex => v0.Rule](
      index => NoInfer(index, NoInferConfig.default),
      index => ExplicitResultTypes(index),
      index => RemoveUnusedImports(index),
      index => RemoveUnusedTerms(index),
      index => NoAutoTupling(index),
      index => Disable(index, DisableConfig.default),
      index => MissingFinal(index)
    )
    semantics.map { fn =>
      val name = fn(v0.SemanticdbIndex.empty).name
      new LegacySemanticRule(name, fn)
    }
  }
}
