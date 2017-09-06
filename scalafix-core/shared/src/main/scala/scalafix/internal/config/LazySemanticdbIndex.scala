package scalafix.internal.config

import scalafix.SemanticdbIndex

// The challenge when loading a rule is that 1) if it's semantic it needs a
// index constructor argument and 2) we don't know upfront if it's semantic.
// For example, to know if a classloaded rules is semantic or syntactic
// we have to test against it's Class[_]. For default rules, the interface
// to detect if a rule is semantic is different.
// LazySemanticdbIndex allows us to delay the computation of a index right up until
// the moment we instantiate the rule.
//type LazySemanticdbIndex = RuleKind => Option[SemanticdbIndex]
class LazySemanticdbIndex(
    f: RuleKind => Option[SemanticdbIndex],
    val reporter: ScalafixReporter)
    extends Function[RuleKind, Option[SemanticdbIndex]] {
  override def apply(v1: RuleKind): Option[SemanticdbIndex] = f(v1)
}

object LazySemanticdbIndex {
  lazy val empty = new LazySemanticdbIndex(_ => None, ScalafixReporter.default)
  def apply(f: RuleKind => Option[SemanticdbIndex]): LazySemanticdbIndex =
    new LazySemanticdbIndex(f, ScalafixReporter.default)
}
