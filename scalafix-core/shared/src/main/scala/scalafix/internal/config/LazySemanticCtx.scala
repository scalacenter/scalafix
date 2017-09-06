package scalafix.internal.config

import scalafix.SemanticCtx

// The challenge when loading a rule is that 1) if it's semantic it needs a
// sctx constructor argument and 2) we don't know upfront if it's semantic.
// For example, to know if a classloaded rules is semantic or syntactic
// we have to test against it's Class[_]. For default rules, the interface
// to detect if a rule is semantic is different.
// LazySemanticCtx allows us to delay the computation of a sctx right up until
// the moment we instantiate the rule.
//type LazySemanticCtx = RewriteKind => Option[SemanticCtx]
class LazySemanticCtx(
    f: RewriteKind => Option[SemanticCtx],
    val reporter: ScalafixReporter)
    extends Function[RewriteKind, Option[SemanticCtx]] {
  override def apply(v1: RewriteKind): Option[SemanticCtx] = f(v1)
}

object LazySemanticCtx {
  lazy val empty = new LazySemanticCtx(_ => None, ScalafixReporter.default)
  def apply(f: RewriteKind => Option[SemanticCtx]): LazySemanticCtx =
    new LazySemanticCtx(f, ScalafixReporter.default)
}
