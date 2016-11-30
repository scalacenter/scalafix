package scalafix.rewrite

import scala.{meta => m}
import scalafix.util.Patch
import scalafix.util.Whitespace

case object ExplicitImplicit extends Rewrite {
  override def rewrite(ast: m.Tree, ctx: RewriteCtx): Seq[Patch] = {
    import scala.meta._
    val semantic = getSemanticApi(ctx)
    def fix(defn: Defn, body: Term): Seq[Patch] = {
      import ctx.tokenList._
      for {
        start <- defn.tokens.headOption
        end <- body.tokens.headOption
        sig = slice(start, end)
        replace <- sig.reverseIterator.find(x =>
          !x.is[Token.Equals] && !x.is[Whitespace])
        typ <- semantic.typeSignature(defn)
      } yield Patch(replace, replace, s"$replace: ${typ.syntax}")
    }.toSeq
    ast.collect {
      case t @ m.Defn.Val(mods, _, None, body)
          if mods.exists(_.syntax == "implicit") =>
        fix(t, body)
      case t @ m.Defn.Def(mods, _, _, _, None, body)
          if mods.exists(_.syntax == "implicit") =>
        fix(t, body)
    }.flatten
  }
}
