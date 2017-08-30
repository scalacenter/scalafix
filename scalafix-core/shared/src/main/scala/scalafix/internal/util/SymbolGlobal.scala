package scalafix.internal.util

import scala.language.experimental.macros

import scala.reflect.macros.blackbox.Context
import scala.util.control.NonFatal
import scala.{meta => m}

object SymbolGlobal {

  /** Construct Symbol.Global from a string literal.
    *
    * This macro creates a Symbol.Global from a string literal. In addition, it allows
    * the following two conveniences:
    *
    * 1. "_root_." is prepended if the symbol does not start with "_"
    * 2. "." is appended if the symbol does not end with either "." or "#"
    *
    * NOTE. This method will likely move to scalameta/scalameta, once
    * https://github.com/scalameta/scalameta/issues/1098 is fixed.
    */
  def apply(symbol: String): m.Symbol.Global = macro impl
  def impl(c: Context)(symbol: c.Tree): c.Tree = {
    import c.universe._
    def convertSignature(sig: m.Signature): c.Tree = sig match {
      case m.Signature.Method(name, jvm) =>
        q"_root_.scala.meta.Signature.Method($name, $jvm)"
      case m.Signature.Term(name) =>
        q"_root_.scala.meta.Signature.Term($name)"
      case m.Signature.Type(name) =>
        q"_root_.scala.meta.Signature.Type($name)"
      case m.Signature.Self(name) =>
        q"_root_.scala.meta.Signature.Self($name)"
      case m.Signature.TermParameter(name) =>
        q"_root_.scala.meta.Signature.TermParameter($name)"
      case m.Signature.TypeParameter(name) =>
        q"_root_.scala.meta.Signature.TypeParameter($name)"
    }
    def convert(sym: m.Symbol): c.Tree = sym match {
      case m.Symbol.None =>
        q"_root_.scala.meta.Symbol.None"
      case m.Symbol.Global(owner, signature) =>
        q"_root_.scala.meta.Symbol.Global(${convert(owner)}, ${convertSignature(signature)})"
      case els =>
        c.abort(c.enclosingPosition, els.structure)
    }
    symbol match {
      case Literal(Constant(symbol: String)) =>
        try {
          var toParse = symbol
          if (!symbol.startsWith("_")) toParse = s"_root_.$toParse"
          if (!symbol.endsWith(".") && !symbol.endsWith("#")) toParse += "."
          m.Symbol(toParse) match {
            case sym @ m.Symbol.Global(_, _) =>
              convert(sym)
            case els =>
              c.abort(
                c.enclosingPosition,
                s"""|Expected: Symbol.Global
                    |Obtained: Symbol.${els.productPrefix}""".stripMargin
              )
          }
        } catch {
          case NonFatal(e) =>
            c.abort(c.enclosingPosition, e.getMessage)
        }
      case els =>
        c.abort(
          c.enclosingPosition,
          "can only convert string literal to symbol")
    }
  }
}
