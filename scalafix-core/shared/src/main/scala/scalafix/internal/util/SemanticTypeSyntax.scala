package scalafix.internal.util

import scala.meta._
import scalafix._
import scalafix.util.SymbolMatcher

object SemanticTypeSyntax {
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
        case Symbol.Global(owner, Signature.Term(_)) => loop(owner)
        case _ => false
      }
      symbol match {
        case Symbol.Global(owner, Signature.Term(_) | Signature.Type(_)) =>
          loop(owner)
        case els =>
          false
      }
    }
    var patch = Patch.empty
    def loop[T](tpe: Tree): T = {
      val result = tpe match {
        case Type.Apply(functionN(_), args) =>
          val rargs = args.map(loop[Type])
          Type.Function(rargs.init, args.last)
        case Type.Apply(tupleN(_), args) =>
          val rargs = args.map(loop[Type])
          Type.Tuple(rargs)
        case Type.Select(_, name @ sctx.Symbol(sym))
            if shortenNames && isStable(sym) =>
          patch += ctx.addGlobalImport(sym)
          name
        case Term.Select(_, name @ sctx.Symbol(sym))
            if shortenNames && isStable(sym) =>
          patch += ctx.addGlobalImport(sym)
          name
        case Type.Apply(qual, args) =>
          val rargs = args.map(loop[Type])
          Type.Apply(loop[Type](qual), rargs)
        case _ =>
          tpe
      }
      result.asInstanceOf[T]
    }
    loop(tpe).asInstanceOf[Type] -> patch
  }
}
