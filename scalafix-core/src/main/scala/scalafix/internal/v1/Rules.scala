package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scala.meta.tokens.Tokens
import scalafix.internal.config.DisableConfig
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.rule._
import scalafix.internal.util.SuppressOps
import scalafix.internal.v0.LegacyRules
import scalafix.internal.v0.LegacySemanticRule
import scalafix.internal.v0.LegacySyntacticRule
import scalafix.lint.RuleDiagnostic
import scalafix.lint.Diagnostic
import scalafix.patch.Patch
import scalafix.rule.RuleName
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
      messages: List[Diagnostic],
      patch: Patch,
      suppress: Boolean): Patch = {
    if (suppress) {
      patch + SuppressOps.addComments(tokens, messages.map(_.position))
    } else {
      patch
    }
  }

  def semanticPatch(
      sdoc: SemanticDoc,
      suppress: Boolean): (String, List[RuleDiagnostic]) = {
    val fixes = rules.iterator.map {
      case rule: SemanticRule =>
        rule.name -> rule.fix(sdoc)
      case rule: SyntacticRule =>
        rule.name -> rule.fix(sdoc.internal.doc)
    }.toMap
    scalafix.Patch.semantic(fixes, sdoc, suppress)
  }

  def syntacticPatch(
      doc: Doc,
      suppress: Boolean): (String, List[RuleDiagnostic]) = {
    require(!isSemantic, semanticRules.map(_.name).mkString("+"))
    val fixes = syntacticRules.iterator.map { rule =>
      rule.name -> rule.fix(doc)
    }.toMap
    scalafix.Patch.syntactic(fixes, doc, suppress)
  }
}

object Rules {
  def allNames: List[String] = defaults.map(_.name.value)
  def allNamesDescriptions: List[(String, String)] = defaults.map { rule =>
    rule.name.value -> rule.description
  }
  def defaults: List[Rule] =
    builtin ++
      legacySemanticRules ++
      legacySyntacticRules

  val builtin: List[Rule] = List(
    RemoveUnusedImports,
    RemoveUnusedTerms,
    NoAutoTupling,
    ProcedureSyntax,
    DisableSyntax(),
    LeakingImplicitClassVal
  )

  val legacySyntacticRules: List[LegacySyntacticRule] = {
    LegacyRules.syntax.map(rule => new LegacySyntacticRule(rule))
  }

  val legacySemanticRules: List[LegacySemanticRule] = {
    val semantics = List[v0.SemanticdbIndex => v0.Rule](
      index => ExplicitResultTypes(index),
      index => Disable(index, DisableConfig.default),
      index => MissingFinal(index)
    )
    semantics.map { fn =>
      val name = fn(v0.SemanticdbIndex.empty).name
      new LegacySemanticRule(name, fn)
    }
  }
}
