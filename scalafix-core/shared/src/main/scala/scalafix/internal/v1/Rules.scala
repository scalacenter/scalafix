package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.internal.config.DisableConfig
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.config.NoInferConfig
import scalafix.internal.rule._
import scalafix.lint.LintMessage
import scalafix.rule.ScalafixRules
import scalafix.util.SemanticdbIndex
import scalafix.v1.Doc
import scalafix.v1.Rule
import scalafix.v1.SemanticDoc
import scalafix.v1.SemanticRule
import scalafix.v1.SyntacticRule

case class Rules(rules: List[Rule] = Nil) {
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
  def semanticPatch(doc: SemanticDoc): (String, List[LintMessage]) = {
    val fixes = rules.iterator.map {
      case rule: SemanticRule =>
        rule.name -> rule.fix(doc)
      case rule: SyntacticRule =>
        rule.name -> rule.fix(doc.doc)
    }.toMap
    scalafix.Patch.apply(fixes, doc.doc.toLegacy, Some(doc.toLegacy))
  }

  def syntacticPatch(doc: Doc): (String, List[LintMessage]) = {
    require(!isSemantic, semanticRules.map(_.name).mkString("+"))
    val fixes = syntacticRules.iterator.map { rule =>
      rule.name -> rule.fix(doc)
    }.toMap
    scalafix.Patch.apply(fixes, doc.toLegacy, None)
  }
}

object Rules {
  def defaults: List[Rule] = legacySemanticRules ++ legacySyntacticRules

  val legacySyntacticRules: List[LegacySyntacticRule] = {
    ScalafixRules.syntax.map(rule => new LegacySyntacticRule(rule))
  }

  val legacySemanticRules: List[LegacySemanticRule] = {
    val semantics = List[SemanticdbIndex => scalafix.Rule](
      index => NoInfer(index, NoInferConfig.default),
      index => ExplicitResultTypes(index),
      index => RemoveUnusedImports(index),
      index => RemoveUnusedTerms(index),
      index => NoAutoTupling(index),
      index => Disable(index, DisableConfig.default),
      index => MissingFinal(index)
    )
    semantics.map { fn =>
      val name = fn(SemanticdbIndex.empty).name
      new LegacySemanticRule(name, fn)
    }
  }
}
