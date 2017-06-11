package scalafix
package rewrite

import scalafix.syntax._
import scala.meta._
import contrib._
import scalafix.util.Whitespace
import scala.collection.immutable.Seq
import scalafix.config.{MemberKind, MemberVisibility}
import scalafix.config.MemberVisibility.Public

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

  def visibility(mod:Mod):Option[MemberVisibility] = Option(mod).flatMap{
    case _:Mod.Private => Some(MemberVisibility.Private)
    case _:Mod.Protected => Some(MemberVisibility.Protected)
    case _ => None
  }

  def kind(defn:Defn):Option[MemberKind] = Option(defn).flatMap{
    case _:Defn.Val => Some(MemberKind.Val)
    case _:Defn.Def => Some(MemberKind.Def)
    case _:Defn.Var => Some(MemberKind.Var)
    case _          => None
  }

  def parseDenotationInfo(denot: Denotation): Option[Type] = {
    val base =
      if (denot.isVal || denot.isVar) denot.info
      else if (denot.isDef) denot.info.replaceFirst(".*\\)", "")
      else {
        throw new UnsupportedOperationException(
          s"Can't parse type for denotation $denot, denot.info=${denot.info}")
      }
    if(denot.isVal || denot.isDef)
      base.parse[Type].toOption
    else
      base.parse[Stat].toOption.flatMap{_.collect{case Term.Ascribe(_,typ) => typ}.headOption}
  }

  def denotations:Seq[(Symbol,Denotation)] = mirror.database.entries.flatMap(_._2.denotations)

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
      } yield {
        ctx.addRight(replace, s": ${typ.treeSyntax}")
      }
    }.to[Seq]

    def checkModsScope(mods:Seq[Mod]):Boolean =
      ctx.config.explicitReturnTypes.memberVisibility.contains(mods.flatMap(visibility).headOption.getOrElse(Public))
    def checkDefnScope(defn:Defn):Boolean =
      kind(defn).exists(ctx.config.explicitReturnTypes.memberKind.contains)

    tree
      .collect {
        case t @ Defn.Val(mods, _, None, body)
          if  t.hasMod(mod"implicit") && !isImplicitly(body)
            || !t.hasMod(mod"implicit") && checkDefnScope(t) && checkModsScope(mods) =>
          fix(t, body)
        case t @ Defn.Var(mods, _, None, Some(body))
          if checkDefnScope(t) && checkModsScope(mods) =>
          fix(t, body)
        case t @ Defn.Def(mods, _, _, _, None, body)
          if t.hasMod(mod"implicit") || checkDefnScope(t) && checkModsScope(mods) =>
          fix(t, body)
      }
      .flatten
      .asPatch
  }
}
