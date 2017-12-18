package scalafix.internal.util

import scala.language.experimental.macros

import scala.reflect.macros.blackbox.Context
import scala.{meta => m}
import scalafix.internal.config.ScalafixMetaconfigReaders
import metaconfig.Conf
import metaconfig.Configured

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
        ScalafixMetaconfigReaders.symbolGlobalReader.read(Conf.Str(symbol)) match {
          case Configured.Ok(sym) => convert(sym)
          case Configured.NotOk(err) =>
            c.abort(
              c.enclosingPosition,
              err.toString()
            )
        }
      case els =>
        c.abort(
          c.enclosingPosition,
          "can only convert string literal to symbol")
    }
  }
}
