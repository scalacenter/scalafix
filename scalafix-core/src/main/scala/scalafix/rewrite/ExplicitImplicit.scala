package scalafix
package rewrite

import scala.meta._
import scalafix.util.Whitespace
import scala.collection.immutable.Seq

case class ExplicitImplicit(mirror: Mirror) extends SemanticRewrite(mirror) {
  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  private def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType(Term.Name("implicitly"), _) => true
    case _ => false
  }
  override def rewrite(ctx: RewriteCtx): Patch = {
    import scala.meta._
    import ctx._
    def fix(defn: Defn, body: Term): Seq[Patch] = {
      import ctx.tokenList._
      for {
        start <- defn.tokens.headOption
        end <- body.tokens.headOption
        // Left-hand side tokens in definition.
        // Example: `val x = ` from `val x = rhs.banana`
        lhsTokens = slice(start, end)
        replace <- lhsTokens.reverseIterator.find(x =>
          !x.is[Token.Equals] && !x.is[Whitespace])
        typ <- {
//          mirror.typeSignature(defn)
          // TODO(olafur) use denotations when the get merged into Mirror.
          Option.empty[Type]
        }
      } yield ctx.addRight(replace, s": ${typ.syntax}")
    }.to[Seq]
    tree
      .collect {
        case t @ Defn.Val(mods, _, None, body)
            if mods.exists(_.syntax == "implicit") &&
              !isImplicitly(body) =>
          fix(t, body)
        case t @ Defn.Def(mods, _, _, _, None, body)
            if mods.exists(_.syntax == "implicit") =>
          fix(t, body)
      }
      .flatten
      .asPatch
    throw new UnsupportedOperationException(
      "ExplicitImplicit is temporarily unsupported while scala.meta.Mirror doesn't support denotations. " +
        "Track https://github.com/scalameta/scalameta/pull/808 to see when denotations get added to Mirror.")
  }
}
