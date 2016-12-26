package scalafix.util

import scala.collection.immutable.Seq
import scala.{meta => m}
import scala.meta._
import scalafix.rewrite._
import syntax._

case class ReplaceTerm(
    original: m.Term,
    replacement: m.Term,
    importSegment: m.Term
) {
  require(replacement.syntax.contains(importSegment.syntax),
          "Specify the portion of the new Term to be imported")
}

class ChangeMethod(ast: m.Tree)(implicit sApi: SemanticApi) {

  import sApi._

  def partialTermMatch(rt: ReplaceTerm)
    : PartialFunction[m.Tree, (scala.meta.Term, ReplaceTerm)] = {
    // only desugar selections
    case t @ q"$expr.$name" & DTerm(desugared)
        if desugared.termNames.map(_.syntax) == rt.original.termNames.map(
          _.syntax) =>
      //I am not sure how to get rid of this asInstanceOf
      //But it should be safe because I have matched on DTerm above
      t.asInstanceOf[m.Term] -> rt
  }

  def partialTermMatches(replacementTerms: Seq[ReplaceTerm])
    : PartialFunction[m.Tree, (m.Term, ReplaceTerm)] =
    replacementTerms.map(partialTermMatch).reduce(_ orElse _)

  def terms(ptm: PartialFunction[m.Tree, (m.Term, ReplaceTerm)]) =
    ast.collect(ptm)

  def termReplacements(trms: Seq[(m.Term, ReplaceTerm)]): Seq[Patch] =
    trms.map {
      case (t, ReplaceTerm(oldTerm, newTerm, imported)) =>
        val replacement = newTerm.termNames
          .map(_.syntax)
          .diff(imported.termNames.map(_.syntax)) //Strip off imported portion
          .filterNot(_ == "apply") //special handling for apply, suppress as it will be inferred
          .mkString(".")
        Patch.replace(t, replacement)
    }

  def gatherPatches(tr: Seq[ReplaceTerm]): Seq[Patch] =
    termReplacements(terms(partialTermMatches(tr)))
}
