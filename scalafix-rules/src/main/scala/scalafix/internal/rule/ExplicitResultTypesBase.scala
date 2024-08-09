package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._

import scalafix.util.TokenOps
import scalafix.v1._

trait Printer {
  def defnType(
      defn: Defn,
      replace: Token,
      space: String
  )(implicit
      ctx: SemanticDocument
  ): Option[Patch]

}

trait ExplicitResultTypesBase[P <: Printer] {

  val config: ExplicitResultTypesConfig

  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly` or `summon`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  protected def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType(Term.Name("implicitly"), _) => true
    case Term.ApplyType(Term.Name("summon"), _) => true
    case _ => false
  }

  protected def defnBody(defn: Defn): Option[Term] = Option(defn).collect {
    case Defn.Val(_, _, _, term) => term
    case Defn.Var(_, _, _, Some(term)) => term
    case Defn.Def(_, _, _, _, _, term) => term
  }

  protected def visibility(mods: Iterable[Mod]): MemberVisibility =
    mods
      .collectFirst {
        case _: Mod.Private => MemberVisibility.Private
        case _: Mod.Protected => MemberVisibility.Protected
      }
      .getOrElse(MemberVisibility.Public)

  protected def kind(defn: Defn): Option[MemberKind] = Option(defn).collect {
    case _: Defn.Val => MemberKind.Val
    case _: Defn.Def => MemberKind.Def
    case _: Defn.Var => MemberKind.Var
  }

  protected def isRuleCandidate[D <: Defn](
      defn: D,
      mods: Iterable[Mod],
      body: Term
  )(implicit ev: Extract[D, Mod]): Boolean = {

    import config._

    def matchesMemberVisibility(): Boolean =
      memberVisibility.contains(visibility(mods))

    def matchesMemberKind(): Boolean =
      kind(defn).exists(memberKind.contains)

    def isFinalLiteralVal: Boolean =
      defn.is[Defn.Val] &&
        mods.exists(_.is[Mod.Final]) &&
        body.is[Lit]

    def matchesSimpleDefinition(): Boolean =
      config.skipSimpleDefinitions.isSimpleDefinition(body)

    def isImplicit: Boolean =
      defn.hasMod(Mod.Implicit()) && !isImplicitly(body)

    def hasParentWihTemplate: Boolean =
      defn.parent.exists(_.is[Template])

    def qualifyingImplicit: Boolean =
      isImplicit && !isFinalLiteralVal

    def matchesConfig: Boolean =
      matchesMemberKind() && matchesMemberVisibility() && !matchesSimpleDefinition()

    def qualifyingNonImplicit: Boolean = {
      !onlyImplicits &&
      hasParentWihTemplate &&
      !defn.hasMod(Mod.Implicit())
    }

    matchesConfig && {
      qualifyingImplicit || qualifyingNonImplicit
    }
  }

  def fixDefinition(defn: Defn, body: Term)(implicit
      ctx: SemanticDocument,
      printer: Printer
  ): Patch = {
    val lst = ctx.tokenList
    val option = SymbolMatcher.exact("scala/Option.")
    val list = SymbolMatcher.exact(
      "scala/package.List.",
      "scala/collection/immutable/List."
    )
    val seq = SymbolMatcher.exact(
      "scala/package.Seq.",
      "scala/collection/Seq.",
      "scala/collection/immutable/Seq."
    )
    def patchEmptyValue(term: Term): Patch = {
      term match {
        case Term.ApplyType(Term.Select(option(_), Term.Name("empty")), _) =>
          Patch.replaceTree(term, "None")
        case Term.ApplyType(Term.Select(list(_), Term.Name("empty")), _) =>
          Patch.replaceTree(term, "Nil")
        case Term.ApplyType(Term.Select(seq(_), Term.Name("empty")), _) =>
          Patch.replaceTree(term, "Nil")
        case _ =>
          Patch.empty
      }
    }
    import lst._
    for {
      start <- defn.tokens.headOption
      end <- body.tokens.headOption
      // Left-hand side tokens in definition.
      // Example: `val x = ` from `val x = rhs.banana`
      lhsTokens = slice(start, end)
      replace <- lhsTokens.reverseIterator.find(x =>
        !x.is[Token.Equals] && !x.is[Trivia]
      )
      space = {
        if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
        else ""
      }
      typePatch <- printer.defnType(defn, replace, space)
      valuePatchOpt = defnBody(defn).map(patchEmptyValue)
    } yield typePatch + valuePatchOpt
  }.asPatch.atomic

  def unsafeFix()(implicit ctx: SemanticDocument, printer: Printer): Patch = {
    ctx.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(_) :: Nil, None, body)
          if isRuleCandidate(t, mods, body) =>
        fixDefinition(t, body)

      case t @ Defn.Var(mods, Pat.Var(_) :: Nil, None, Some(body))
          if isRuleCandidate(t, mods, body) =>
        fixDefinition(t, body)

      case t @ Defn.Def(mods, _, _, _, None, body)
          if isRuleCandidate(t, mods, body) =>
        fixDefinition(t, body)
    }.asPatch
  }
}

object ExplicitResultTypesBase {

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Var(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Def(_, name, _, _, _, _) => name
  }
}
