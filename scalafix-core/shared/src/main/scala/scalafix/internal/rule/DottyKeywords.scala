package scalafix.internal.rule

import scala.meta._
import scalafix.Patch
import scalafix.rule.Rule
import scalafix.rule.RewriteCtx
import scalafix.syntax._

case object DottyKeywords extends Rule {
  def name = "DottyKeywords"
  override def fix(ctx: RewriteCtx): Patch =
    ctx.tree.collect {
      case name @ Name("enum") =>
        ctx.replaceTree(name, s"`enum`")
      case name @ Name("inline") if !name.parents.exists(_.is[Mod.Annot]) =>
        ctx.replaceTree(name, s"`inline`")
    }.asPatch
}
