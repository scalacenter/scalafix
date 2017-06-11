package scalafix
package rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.contrib._
import scalafix.config.{MemberKind, MemberVisibility}
import scalafix.syntax._
import scalafix.util.Whitespace

// TODO: implement ExplicitReturnTypesConfig
case class ExplicitReturnTypes(mirror: Mirror)
    extends SemanticRewrite(mirror) {
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
    case Defn.Var(_, Seq(Pat.Var.Term(name)), _, _) => name
    case Defn.Def(_, name, _, _, _, _) => name
  }

  def visibility(mods: Traversable[Mod]): MemberVisibility =
    mods
      .collect {
        case _: Mod.Private => MemberVisibility.Private
        case _: Mod.Protected => MemberVisibility.Protected
      }
      .headOption
      .getOrElse(MemberVisibility.Public)

  def kind(defn: Defn): Option[MemberKind] = Option(defn).collect {
    case _: Defn.Val => MemberKind.Val
    case _: Defn.Def => MemberKind.Def
    case _: Defn.Var => MemberKind.Var
  }

  def parseDenotationInfo(denot: Denotation): Option[Type] = {
    val base =
      if (denot.isVal || denot.isVar) denot.info
      else if (denot.isDef) denot.info.replaceFirst(".*\\)", "")
      else {
        throw new UnsupportedOperationException(
          s"Can't parse type for denotation $denot, denot.info=${denot.info}")
      }
    if (denot.isVal || denot.isDef)
      base.parse[Type].toOption
    else
      base.parse[Stat].toOption.flatMap {
        _.collect { case Term.Ascribe(_, typ) => typ }.headOption
      }
  }

  def defnType(defn: Defn): Option[Type] =
    for {
      name <- defnName(defn)
      symbol <- name.symbolOpt
      denot <- mirror.database.denotations.get(symbol)
      typ <- parseDenotationInfo(denot)
    } yield typ

  override def rewrite(ctx: RewriteCtx): Patch = {
    import ctx._

    import scala.meta._
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

    def checkModsScope(mods: Seq[Mod]): Boolean =
      ctx.config.explicitReturnTypes.memberVisibility
        .contains(visibility(mods))

    def checkDefnScope(defn: Defn): Boolean =
      kind(defn).exists(ctx.config.explicitReturnTypes.memberKind.contains)

    tree
      .collect {
        case t @ Defn.Val(mods, _, None, body)
            if t.hasMod(mod"implicit") && !isImplicitly(body)
              || !t.hasMod(mod"implicit")
                && checkDefnScope(t) && checkModsScope(mods) =>
          fix(t, body)
        case t @ Defn.Var(mods, _, None, Some(body))
            if checkDefnScope(t) && checkModsScope(mods) =>
          fix(t, body)
        case t @ Defn.Def(mods, _, _, _, None, body)
            if t.hasMod(mod"implicit")
              || checkDefnScope(t) && checkModsScope(mods) =>
          fix(t, body)
      }
      .flatten
      .asPatch
  }
}
