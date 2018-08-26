package scalafix.internal.rule

import scala.meta._
import scalafix.v1._
import scalafix.syntax._

class DottyKeywords extends SyntacticRule("DottyKeywords") {

  override def description: String =
    "Rewrite that replaces enum and inline with `enum` and `inline` for compatibility with Dotty"

  override def fix(implicit doc: Doc): Patch =
    doc.tree.collect {
      case name @ Name("enum") =>
        Patch.replaceTree(name, s"`enum`")
      case name @ Name("inline") if !name.parents.exists(_.is[Mod.Annot]) =>
        Patch.replaceTree(name, s"`inline`")
    }.asPatch
}
