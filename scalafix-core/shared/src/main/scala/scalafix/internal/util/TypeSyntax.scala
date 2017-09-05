package scalafix.internal.util

import scala.meta._
import scalafix._
import scalafix.internal.util.SymbolOps.SymbolType
import scalafix.util.SymbolMatcher
import scalafix.util.TreeExtractors._
import org.langmeta.semanticdb.Symbol

object TypeSyntax {
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
        case _ =>
          false
      }
    }

    def stableRef(sym: Symbol): (Patch, Type) = {
      var patch = Patch.empty
      def loop[T](symbol: Symbol): T = {
        val result = symbol match {
          case Symbol.Global(Symbol.None, Signature.Term(name)) =>
            Term.Name(name)
          case Symbol.Global(owner @ SymbolType(), Signature.Type(name)) =>
            Type.Project(loop[Type](owner), Type.Name(name))
          case Symbol.Global(owner, Signature.Term(name)) =>
            if (shortenNames && isStable(owner)) {
              patch += ctx.addGlobalImport(symbol)
              Term.Name(name)
            } else Term.Select(loop(owner), Term.Name(name))
          case Symbol.Global(owner, Signature.Type(name)) =>
            if (shortenNames && isStable(owner)) {
              patch += ctx.addGlobalImport(symbol)
              Type.Name(name)
            } else Type.Select(loop(owner), Type.Name(name))
          case Symbol.Global(_, Signature.TypeParameter(name)) =>
            Type.Name(name)
        }
        result.asInstanceOf[T]
      }
      val tpe = loop[Type](sym)
      patch -> tpe
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

        case Type.Name(_) :&&: sctx.Symbol(sym) =>
          val (addImport, tpe) = stableRef(sym)
          patch += addImport
          tpe

        // TODO(olafur): handle select after https://github.com/scalameta/scalameta/pull/1107

        // Recursive case
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
