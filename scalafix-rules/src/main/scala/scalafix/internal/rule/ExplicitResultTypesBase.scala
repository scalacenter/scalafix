package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scala.meta.tokens.Token

import buildinfo.RulesBuildInfo
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

abstract class ExplicitResultTypesBase[P <: Printer]
    extends SemanticRule("ExplicitResultTypes") {

  override def description: String =
    "Inserts type annotations for inferred public members."

  override def isRewrite: Boolean = true

  val config: ExplicitResultTypesConfig

  protected val compilerScalaVersion: String =
    RulesBuildInfo.scalaVersion

  protected def stripPatchVersion(v: String): String =
    v.split('.').take(2).mkString(".")

  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly` or `summon`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  protected def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType.Initial(Term.Name("implicitly" | "summon"), _) => true
    case _ => false
  }

  protected def defnBody(defn: Defn): Option[Term] = defn match {
    case t: Defn.Val => Some(t.rhs)
    case t: Defn.Var => Some(t.body)
    case t: Defn.Def => Some(t.body)
    case _ => None
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
      defn.parent.exists(_.is[Template.Body])

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
        case Term.ApplyType.Initial(tf: Term.Select, _)
            if tf.name.value == "empty" =>
          val qual = tf.qual.symbol
          if (option.matches(qual)) Patch.replaceTree(term, "None")
          else if (list.matches(qual) || seq.matches(qual))
            Patch.replaceTree(term, "Nil")
          else Patch.empty
        case _ => Patch.empty
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
      case t @ Defn.Val(mods, (_: Pat.Var) :: Nil, None, body)
          if isRuleCandidate(t, mods, body) =>
        fixDefinition(t, body)

      case t @ Defn.Var.Initial(mods, (_: Pat.Var) :: Nil, None, Some(body))
          if isRuleCandidate(t, mods, body) =>
        fixDefinition(t, body)

      case t: Defn.Def
          if t.decltpe.isEmpty &&
            isRuleCandidate(t, t.mods, t.body) =>
        fixDefinition(t, t.body)
    }.asPatch
  }
}

object ExplicitResultTypesBase {

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Var.Initial(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Def.Initial(_, name, _, _, _, _) => name
  }
}
