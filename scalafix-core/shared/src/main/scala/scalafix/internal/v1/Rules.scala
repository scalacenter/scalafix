package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Configured
import scalafix.internal.config.MetaconfigPendingUpstream
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
  def semanticPatch(doc: SemanticDoc): String = {
    ???
  }
  def syntacticPatch(doc: Doc): String = {
    require(!isSemantic, semanticRules.map(_.name).mkString("+"))
    val fixes = syntacticRules.iterator.map { rule =>
      rule.name -> rule.fix(doc)
    }.toMap
//    Patch.apply(
//      fixes,
//    )
    ???
  }
}

object Rules {
  val defaults: List[Rule] = Nil
}
