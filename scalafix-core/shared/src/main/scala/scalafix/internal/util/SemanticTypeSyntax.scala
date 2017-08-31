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
        // Function2[A, B] => A => B
        case Type.Apply(functionN(_), args) =>
          val rargs = args.map(loop[Type])
          Type.Function(rargs.init, rargs.last)

        // Tuple2[A, B] => (A, B)
        case Type.Apply(tupleN(_), args) =>
          val rargs = args.map(loop[Type])
          Type.Tuple(rargs)

        // shorten names
        case Type.Select(_, name @ sctx.Symbol(sym))
            if shortenNames && isStable(sym) =>
          patch += ctx.addGlobalImport(sym)
          name
        case Term.Select(_, name @ sctx.Symbol(sym))
            if shortenNames && isStable(sym) =>
          patch += ctx.addGlobalImport(sym)
          name

        // _root_ qualify names
        case Term.Select(qual @ Term.Name(root), name)
            if !shortenNames && root != "_root_" =>
          Term.Select(q"_root_.$qual", name)
        case Type.Select(qual @ Term.Name(root), name)
            if !shortenNames && root != "_root_" =>
          Type.Select(q"_root_.$qual", name)

        // Recursive cases
        case Type.Select(qual, name) =>
          Type.Select(loop[Term.Ref](qual), name)
        case Term.Select(qual, name) =>
          Term.Select(loop[Term.Ref](qual), name)
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
