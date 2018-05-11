package scalafix.internal.v1

import scalafix.patch.Patch
import scalafix.v1.Doc
import scalafix.v1.Rule
import scalafix.v1.SemanticDoc
import scalafix.v1.SemanticRule
import scalafix.v1.SyntacticRule

case class Rules(rules: List[Rule] = Nil) {
  def isEmpty: Boolean = rules.isEmpty
  def isSemantic: Boolean = semanticRules.nonEmpty
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
