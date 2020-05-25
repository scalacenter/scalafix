package scalafix

import scala.meta._
import scala.meta.internal.scalafix.ScalafixScalametaHacks
import scalafix.internal.util.DenotationOps
import scalafix.internal.util.SymbolOps
import scalafix.util.SymbolMatcher
import scalafix.util.TreeOps
import scalafix.v0.Symbol
import scala.collection.compat.immutable.LazyList

package object syntax {
  implicit class XtensionRefSymbolOpt(tree: Tree)(
      implicit index: v0.SemanticdbIndex
  ) {
    def symbol: Option[Symbol] = index.symbol(tree.pos)
    def denotation: Option[v0.Denotation] = index.denotation(tree)
  }
  implicit class XtensionParsedOpt[T](parsed: Parsed[T]) {
    def toOption: Option[T] = parsed match {
      case parsers.Parsed.Success(tree) => Some(tree)
      case _ => None
    }
  }
  implicit class XtensionSymbolSemanticdbIndex(symbol: Symbol)(
      implicit index: v0.SemanticdbIndex
  ) {
    def denotation: Option[v0.Denotation] = index.denotation(symbol)
    def resultType: Option[Type] =
      denotation.flatMap(denot =>
        DenotationOps.resultType(symbol, denot, DenotationOps.defaultDialect)
      )
  }
  implicit class XtensionSymbol(symbol: Symbol) {
    def normalized: Symbol = SymbolOps.normalize(symbol)
  }
  implicit class XtensionDocument(document: v0.Document) {
    def dialect: Dialect = ScalafixScalametaHacks.dialect(document.language)
  }
  implicit class XtensionTreeScalafix(tree: Tree) {
    def matches(matcher: SymbolMatcher): Boolean =
      matcher.matches(tree)
    def parents: LazyList[Tree] = TreeOps.parents(tree)
    def input: Input = tree.tokens.headOption.map(_.input).getOrElse(Input.None)
  }
  implicit class XtensionInputScalafix(input: Input) {
    def label: String = input match {
      case inputs.Input.File(path, _) => path.toString()
      case inputs.Input.VirtualFile(label, _) => label
      case _ =>
        s"Input.${input.productPrefix}('<${input.chars.take(10).mkString}...>')"
          .replace(System.lineSeparator(), "")
    }
  }
}
