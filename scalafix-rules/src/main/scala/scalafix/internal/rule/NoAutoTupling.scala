package scalafix.internal.rule

import scala.meta._
import scalafix.v1._
import scala.collection.compat._ // Used for cross-compilation.

class NoAutoTupling extends SemanticRule("NoAutoTupling") {

  override def description: String =
    "Inserts explicit tuples for adapted argument lists for compatibility with -Yno-adapted-args"
  override def isRewrite: Boolean = true

  private[this] def addWrappingParens(args: Seq[Term]): Patch =
    Patch.addLeft(args.head.tokens.head, "(") +
      Patch.addRight(args.last.tokens.last, ")")

  private[this] def insertUnit(t: Term.Apply): Patch =
    Patch.addRight(t.tokens.init.last, "()")

  override def fix(implicit doc: SemanticDocument): Patch = {
    val unitAdaptations: Set[Position] =
      doc.diagnostics.iterator.collect {
        case message
            if message.message.startsWith(
              "Adaptation of argument list by inserting ()"
            ) =>
          message.position
      }.toSet

    val tupleAdaptations: Set[Position] =
      doc.diagnostics.iterator.collect {
        case message
            if message.message.startsWith(
              "Adapting argument list by creating"
            ) || message.message.startsWith(
              "adapted the argument list to the expected"
            ) =>
          message.position
      }.toSet
    doc.tree
      .collect {
        case t: Term.Apply if tupleAdaptations.contains(t.pos) =>
          addWrappingParens(t.args)
        case t: Term.Apply
            if t.args.isEmpty && unitAdaptations.contains(t.pos) =>
          insertUnit(t)
      }
      .map(_.atomic)
      .asPatch
  }
}
