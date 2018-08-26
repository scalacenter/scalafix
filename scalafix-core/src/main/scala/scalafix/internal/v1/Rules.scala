package scalafix.internal.v1

import java.util.ServiceLoader
import metaconfig.Configured
import scala.meta.tokens.Tokens
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.util.SuppressOps
import scalafix.lint.RuleDiagnostic
import scalafix.lint.Diagnostic
import scalafix.patch.Patch
import scalafix.rule.RuleName
import scalafix.v1.Configuration
import scalafix.v1.Doc
import scalafix.v1.Rule
import scalafix.v1.SemanticDoc
import scalafix.v1.SemanticRule
import scalafix.v1.SyntacticRule

case class Rules(rules: List[Rule] = Nil) {

  def name: RuleName = RuleName(rules.flatMap(_.name.identifiers))
  def isEmpty: Boolean = rules.isEmpty
  def isSemantic: Boolean = semanticRules.nonEmpty
  def withConfiguration(config: Configuration): Configured[Rules] =
    MetaconfigPendingUpstream
      .traverse(rules.map(_.withConfiguration(config)))
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
  def all(): List[Rule] = {
    all(Thread.currentThread.getContextClassLoader)
  }
  def all(classLoader: ClassLoader): List[Rule] = {
    import scala.collection.JavaConverters._
    ServiceLoader
      .load(classOf[Rule], classLoader)
      .iterator()
      .asScala
      .toList
  }
}
