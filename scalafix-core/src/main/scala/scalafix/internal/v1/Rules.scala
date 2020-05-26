package scalafix.internal.v1

import java.util.ServiceLoader
import metaconfig.Configured
import scala.meta.tokens.Tokens
import scalafix.internal.config.MetaconfigOps
import scalafix.internal.patch.PatchInternals
import scalafix.internal.util.SuppressOps
import scalafix.lint.Diagnostic
import scalafix.lint.RuleDiagnostic
import scalafix.patch.Patch
import scalafix.rule.RuleName
import scalafix.v1.Configuration
import scalafix.v1.Rule
import scalafix.v1.SemanticDocument
import scalafix.v1.SemanticRule
import scalafix.v1.SyntacticDocument
import scalafix.v1.SyntacticRule
import scala.util.control.NonFatal
import scala.collection.mutable

case class Rules(rules: List[Rule] = Nil) {

  def name: RuleName = RuleName(rules.flatMap(_.name.identifiers))
  def isEmpty: Boolean = rules.isEmpty
  def isSemantic: Boolean = semanticRules.nonEmpty
  def withConfiguration(config: Configuration): Configured[Rules] =
    MetaconfigOps
      .traverse(rules.map(_.withConfiguration(config)))
      .map(Rules(_))
  def semanticRules: List[SemanticRule] = rules.collect {
    case s: SemanticRule => s
  }
  def syntacticRules: List[SyntacticRule] = rules.collect {
    case s: SyntacticRule => s
  }
  def afterComplete(): List[(Rule, Throwable)] = {
    foreachRule(_.afterComplete())
  }
  def beforeStart(): List[(Rule, Throwable)] = {
    foreachRule(_.beforeStart())
  }

  private def foreachRule(fn: Rule => Unit): List[(Rule, Throwable)] = {
    val buf = mutable.ListBuffer.empty[(Rule, Throwable)]
    rules.foreach { rule =>
      try fn(rule)
      catch {
        case NonFatal(e) =>
          buf += (rule -> e)
      }
    }
    buf.result()
  }

  def addSuppression(
      tokens: Tokens,
      messages: List[Diagnostic],
      patch: Patch,
      suppress: Boolean
  ): Patch = {
    if (suppress) {
      patch + SuppressOps.addComments(tokens, messages.map(_.position))
    } else {
      patch
    }
  }

  def semanticPatch(
      sdoc: SemanticDocument,
      suppress: Boolean
  ): (String, List[RuleDiagnostic]) = {
    val fixes = rules.map {
      case rule: SemanticRule =>
        rule.name -> rule.fix(sdoc)
      case rule: SyntacticRule =>
        rule.name -> rule.fix(sdoc.internal.doc)
    }.toMap
    PatchInternals.semantic(fixes, sdoc, suppress)
  }

  def syntacticPatch(
      doc: SyntacticDocument,
      suppress: Boolean
  ): (String, List[RuleDiagnostic]) = {
    require(!isSemantic, semanticRules.map(_.name).mkString("+"))
    val fixes = syntacticRules.map { rule =>
      rule.name -> rule.fix(doc)
    }.toMap
    PatchInternals.syntactic(fixes, doc, suppress)
  }
}

object Rules {
  def all(): List[Rule] = {
    all(Thread.currentThread.getContextClassLoader)
  }
  def all(classLoader: ClassLoader): List[Rule] = {
    import scala.jdk.CollectionConverters._
    ServiceLoader
      .load(classOf[Rule], classLoader)
      .iterator()
      .asScala
      .toList
  }
}
