package scalafix

import java.nio.charset.Charset
import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.semanticdb.Signature
import scala.meta.semanticdb.Symbol
import scala.util.Try
import org.scalameta.logger
import scala.compat.Platform.EOL
import scala.meta.internal.scalafix.ScalafixScalametaHacks
import scalafix.internal.util.SymbolOps
import scalafix.util.TreeOps

package object syntax {

  implicit class XtensionRefSymbolOpt(ref: Ref)(implicit mirror: Database) {
    def symbolOpt: Option[Symbol] = mirror.names.collectFirst {
      case ResolvedName(pos, sym, _) if pos == ref.pos => sym
    }
    def symbol: Symbol = symbolOpt.get
  }

  implicit class XtensionParsedOpt[T](parsed: Parsed[T]) {
    def toOption: Option[T] = parsed match {
      case parsers.Parsed.Success(tree) => Some(tree)
      case _ => None
    }
  }

  implicit class XtensionSymbolMirror(symbol: Symbol)(implicit mirror: Database) {
    def denotOpt: Option[Denotation] = mirror.symbols.collectFirst {
      case ResolvedSymbol(sym, denot) if sym == symbol => denot
    }
  }
  implicit class XtensionSymbol(symbol: Symbol) {
    private def underlyingSymbols(symbol: Symbol): Seq[Symbol] = symbol match {
      case Symbol.Multi(symbols) => symbols
      case _ => List(symbol)
    }
    def isSameNormalized(other: Symbol): Boolean = {
      val syms = underlyingSymbols(symbol).map(_.normalized)
      val otherSyms = underlyingSymbols(other).map(_.normalized)
      syms.exists(otherSyms.contains)
    }
    def normalized: Symbol = SymbolOps.normalize(symbol)
  }
  implicit class XtensionAttributes(attributes: Attributes) {
    def dialect: Dialect = ScalafixScalametaHacks.dialect(attributes.language)
  }
  implicit class XtensionTreeScalafix(tree: Tree) {
    def parents: Stream[Tree] = TreeOps.parents(tree)
    def input: Input = tree.tokens.head.input
  }
  implicit class XtensionInputScalafix(input: Input) {
    def label: String = input match {
      case inputs.Input.File(path, _) => path.toString()
      case inputs.Input.VirtualFile(label, _) => label
      case _ =>
        s"Input.${input.productPrefix}('<${input.chars.take(10).mkString}...>')"
          .replaceAllLiterally(EOL, "")
    }
  }
}
