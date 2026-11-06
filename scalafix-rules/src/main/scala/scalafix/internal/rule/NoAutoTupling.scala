package scalafix.internal.rule

import scala.collection.mutable

import scala.meta._

import scalafix.v1._

class NoAutoTupling extends SemanticRule("NoAutoTupling") {

  override def description: String =
    "Inserts explicit tuples for adapted argument lists for compatibility with -Yno-adapted-args"
  override def isRewrite: Boolean = true

  private def addWrappingParens(args: Seq[Term]): Patch =
    Patch.addLeft(args.head.tokens.head, "(") +
      Patch.addRight(args.last.tokens.last, ")")

  private def insertUnit(t: Term.Apply): Patch =
    Patch.addRight(t.tokens.init.last, "()")

  // newer scalameta moves the range from the application to its argument clause
  private def isAdapted(
      adaptations: mutable.LongMap[Int],
      t: Term.Apply
  ): Boolean = {
    val end = t.pos.end
    val len = adaptations.getOrElse(end, -1)
    len >= 0 && {
      val maybeBeg = end - len
      maybeBeg == t.argClause.pos.start ||
      maybeBeg == t.pos.start
    }
  }

  private def add(adaptations: mutable.LongMap[Int], pos: Position): Unit =
    adaptations(pos.end) = pos.end - pos.start

  override def fix(implicit doc: SemanticDocument): Patch = {
    val unitAdaptations = mutable.LongMap.empty[Int]
    val tupleAdaptations = mutable.LongMap.empty[Int]
    doc.diagnostics.foreach { diagnostic =>
      val msg = diagnostic.message
      if (
        msg.startsWith("Adaptation of argument list by inserting ()") ||
        msg.startsWith("adaptation of an empty argument list by inserting ()")
      ) add(unitAdaptations, diagnostic.position)
      else if (
        msg.startsWith("Adapting argument list by creating") ||
        msg.startsWith("adapted the argument list to the expected")
      ) add(tupleAdaptations, diagnostic.position)
    }
    doc.tree
      .collect {
        case t: Term.Apply if isAdapted(tupleAdaptations, t) =>
          addWrappingParens(t.argClause.values)
        case t: Term.Apply
            if t.argClause.isEmpty && isAdapted(unitAdaptations, t) =>
          insertUnit(t)
      }
      .map(_.atomic)
      .asPatch
  }
}
