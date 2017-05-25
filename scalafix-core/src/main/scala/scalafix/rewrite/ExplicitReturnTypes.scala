package scalafix
package rewrite

import scalafix.syntax._
import scala.meta._
import scalafix.util.Whitespace
import scala.collection.immutable.Seq
import org.scalameta.logger

case class ExplicitReturnTypes(mirror: Mirror) extends SemanticRewrite(mirror) {
  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  private def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType(Term.Name("implicitly"), _) => true
    case _ => false
  }

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Seq(Pat.Var.Term(name)), _, _) => name
    case Defn.Def(_, name, _, _, _, _) => name
  }

  def parseDenotationInfo(denot: Denotation): Option[Type] = {
    val base =
      if (denot.isVal) denot.info
      else if (denot.isDef) denot.info.replaceFirst(".*\\)", "")
      else {
        throw new UnsupportedOperationException(
          s"Can't parse type for denotation $denot, denot.info=${denot.info}")
      }
    base.parse[Type].toOption
  }

  def defnType(defn: Defn): Option[Type] =
    for {
      name <- defnName(defn)
      symbol <- name.symbolOpt
      denot <- mirror.database.denotations.get(symbol)
      typ <- parseDenotationInfo(denot)
    } yield typ

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
        typ <- defnType(defn)
      } yield ctx.addRight(replace, s": ${typ.treeSyntax}")
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
  }
}
