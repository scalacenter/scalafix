package scalafix.internal.util

import scala.meta._
import scalafix._
import scalafix.util.SymbolMatcher
import org.scalameta.logger

object TypeTreeOps {
  def prettify(tpe: Type, ctx: RewriteCtx, shortenNames: Boolean)(
      implicit sctx: SemanticCtx): (Type, Patch) = {
    val functionN: SymbolMatcher = SymbolMatcher.exact(
      1.to(22).map(i => Symbol(s"_root_.scala.Function$i#")): _*
    )
    val tupleN: SymbolMatcher = SymbolMatcher.exact(
      1.to(22).map(i => Symbol(s"_root_.scala.Tuple$i#")): _*
    )

    def isStable(symbol: Symbol): Boolean = {
      def loop(symbol: Symbol): Boolean = symbol match {
        case Symbol.None => true
        case Symbol.Global(owner, Signature.Term(_)) => isStable(owner)
        case _ => false
      }
      val result = symbol match {
        case Symbol.Global(owner, Signature.Term(_) | Signature.Type(_)) =>
          loop(owner)
        case _ => false
      }
      result
    }
    var patch = Patch.empty
    def loop(tpe: Type): Type = tpe match {
      case Type.Apply(functionN(_), args) =>
        Type.Function(args.init.map(loop), loop(args.last))
      case Type.Apply(tupleN(_), args) =>
        Type.Tuple(args.map(loop))
      case Type.Select(qual, name @ sctx.Symbol(sym))
          if shortenNames && isStable(sym) =>
        patch += ctx.addGlobalImport(sym)
        name
      case Type.Apply(tpe, args) =>
        logger.elem(tpe)
        Type.Apply(loop(tpe), args.map(loop))
      case _ =>
        tpe
    }
    loop(tpe) -> patch
  }
}
