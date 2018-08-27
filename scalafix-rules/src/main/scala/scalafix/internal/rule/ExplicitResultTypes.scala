package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.scalafix.ScalafixScalametaHacks
import scalafix.patch.Patch
import scalafix.v1._
import scalafix.util.TokenOps
import metaconfig.Configured
import scalafix.internal.util.PrettyResult
import scalafix.internal.util.QualifyStrategy
import scalafix.internal.util.PrettyType
import scalafix.v1.MissingSymbolException

case class ExplicitResultTypes(config: ExplicitResultTypesConfig)
    extends SemanticRule("ExplicitResultTypes") {

  def this() = this(ExplicitResultTypesConfig.default)

  override def description: String =
    "Rewrite that inserts explicit type annotations for def/val/var"

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf // Support deprecated explicitReturnTypes config
      .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
        ExplicitResultTypesConfig.default
      )
      .map(c => ExplicitResultTypes(c))

  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  private def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType(Term.Name("implicitly"), _) => true
    case _ => false
  }

  def defnName(defn: Defn): Option[Name] = Option(defn).collect {
    case Defn.Val(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Var(_, Pat.Var(name) :: Nil, _, _) => name
    case Defn.Def(_, name, _, _, _, _) => name
  }

  def visibility(mods: Traversable[Mod]): MemberVisibility =
    mods
      .collectFirst {
        case _: Mod.Private => MemberVisibility.Private
        case _: Mod.Protected => MemberVisibility.Protected
      }
      .getOrElse(MemberVisibility.Public)

  def kind(defn: Defn): Option[MemberKind] = Option(defn).collect {
    case _: Defn.Val => MemberKind.Val
    case _: Defn.Def => MemberKind.Def
    case _: Defn.Var => MemberKind.Var
  }
  import scala.meta.internal.{semanticdb => s}
  def unsafeToType(
      ctx: SemanticDoc,
      pos: Position,
      symbol: Symbol
  ): PrettyResult[Type] = {
    val info = ctx.internal.symtab
      .info(symbol.value)
      .getOrElse(throw new NoSuchElementException(symbol.value))
    val tpe = info.signature match {
      case method: s.MethodSignature =>
        method.returnType
      case value: s.ValueSignature =>
        value.tpe
      case els =>
        throw new IllegalArgumentException(s"Unsupported signature $els")
    }
    PrettyType.toType(
      tpe,
      ctx.internal.symtab,
      if (config.unsafeShortenNames) QualifyStrategy.Readable
      else QualifyStrategy.Full,
      fatalErrors = config.fatalWarnings
    )
  }

  def toType(
      pos: Position,
      symbol: Symbol
  )(implicit ctx: SemanticDoc): Option[PrettyResult[Type]] = {
    try {
      Some(unsafeToType(ctx, pos, symbol))
    } catch {
      case e: MissingSymbolException =>
        if (config.fatalWarnings) {
          ctx.internal.config.reporter.error(e.getMessage, pos)
        } else {
          // Silently discard failures from producing a new type.
          // Errors are most likely caused by known upstream issue that have been reported in Scalameta.
        }
        None
    }
  }

  override def fix(implicit ctx: SemanticDoc): Patch = {
    def defnType(defn: Defn): Option[(Type, Patch)] =
      for {
        name <- defnName(defn)
        defnSymbol <- name.symbol.asNonEmpty
        result <- toType(name.pos, defnSymbol)
      } yield {
        val addGlobalImports = result.imports.map { s =>
          val symbol = Symbol(s)
          Patch.addGlobalImport(symbol)
        }
        result.tree -> addGlobalImports.asPatch
      }
    import scala.meta._
    def fix(defn: Defn, body: Term): Patch = {
      val lst = ctx.tokenList
      import lst._
      for {
        start <- defn.tokens.headOption
        end <- body.tokens.headOption
        // Left-hand side tokens in definition.
        // Example: `val x = ` from `val x = rhs.banana`
        lhsTokens = slice(start, end)
        replace <- lhsTokens.reverseIterator.find(
          x => !x.is[Token.Equals] && !x.is[Trivia]
        )
        (typ, patch) <- defnType(defn)
        space = {
          if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
          else ""
        }
      } yield Patch.addRight(replace, s"$space: ${treeSyntax(typ)}") + patch
    }.asPatch.atomic

    def treeSyntax(tree: Tree): String =
      ScalafixScalametaHacks.resetOrigin(tree).syntax

    def isRuleCandidate[D <: Defn](
        defn: D,
        nm: Name,
        mods: Traversable[Mod],
        body: Term
    )(implicit ev: Extract[D, Mod]): Boolean = {
      import config._

      def matchesMemberVisibility(): Boolean =
        memberVisibility.contains(visibility(mods))

      def matchesMemberKind(): Boolean =
        kind(defn).exists(memberKind.contains)

      def matchesSimpleDefinition(): Boolean =
        body.is[Lit] && skipSimpleDefinitions

      def isImplicit: Boolean =
        defn.hasMod(mod"implicit") && !isImplicitly(body)

      def hasParentWihTemplate: Boolean =
        defn.parent.exists(_.is[Template])

      def isLocal: Boolean =
        if (config.skipLocalImplicits) nm.symbol.isLocal
        else false

      isImplicit && !isLocal || {
        hasParentWihTemplate &&
        !defn.hasMod(mod"implicit") &&
        !matchesSimpleDefinition() &&
        matchesMemberKind() &&
        matchesMemberVisibility()
      }
    }

    ctx.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body)
          if isRuleCandidate(t, name, mods, body) =>
        fix(t, body)

      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body))
          if isRuleCandidate(t, name, mods, body) =>
        fix(t, body)

      case t @ Defn.Def(mods, name, _, _, None, body)
          if isRuleCandidate(t, name, mods, body) =>
        fix(t, body)
    }.asPatch
  }
}
