package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scalafix.patch.Patch
import scalafix.v1._
import scalafix.util.TokenOps
import metaconfig.Configured
import scala.meta.internal.pc.ScalafixGlobal
import scalafix.internal.v1.LazyValue
import scala.util.control.NonFatal

final class ExplicitResultTypes(
    config: ExplicitResultTypesConfig,
    global: LazyValue[Option[ScalafixGlobal]]
) extends SemanticRule("ExplicitResultTypes") {

  def this() = this(ExplicitResultTypesConfig.default, LazyValue.now(None))

  override def description: String =
    "Inserts explicit annotations for inferred types of def/val/var"
  override def isRewrite: Boolean = true
  override def isExperimental: Boolean = true

  override def afterComplete(): Unit = {
    shutdownCompiler()
  }

  private def shutdownCompiler(): Unit = {
    global.foreach(_.foreach(g => {
      try {
        g.askShutdown()
        g.close()
      } catch {
        case NonFatal(_) =>
      }
    }))
  }

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val newGlobal: LazyValue[Option[ScalafixGlobal]] =
      if (config.scalacClasspath.isEmpty) LazyValue.now(None)
      else {
        LazyValue.fromUnsafe { () =>
          ScalafixGlobal.newCompiler(
            config.scalacClasspath,
            config.scalacOptions
          )
        }
      }
    config.conf // Support deprecated explicitReturnTypes config
      .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
        ExplicitResultTypesConfig.default
      )
      .map(c => new ExplicitResultTypes(c, newGlobal))
  }

  override def fix(implicit ctx: SemanticDocument): Patch = {
    try unsafeFix()
    catch {
      case _: CompilerException =>
        shutdownCompiler()
        global.restart()
        try unsafeFix()
        catch {
          case _: CompilerException =>
            // Ignore compiler crashes.
            Patch.empty
        }
    }
  }
  def unsafeFix()(implicit ctx: SemanticDocument): Patch = {
    lazy val types = {
      pprint.log(global.value.map(_.isHijacked()))
      TypeRewrite(global.value)
    }
    ctx.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body)
          if isRuleCandidate(t, name, mods, body) =>
        fixDefinition(t, body, types)

      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body))
          if isRuleCandidate(t, name, mods, body) =>
        fixDefinition(t, body, types)

      case t @ Defn.Def(mods, name, _, _, None, body)
          if isRuleCandidate(t, name, mods, body) =>
        fixDefinition(t, body, types)
    }.asPatch
  }

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

  def isRuleCandidate[D <: Defn](
      defn: D,
      nm: Name,
      mods: Traversable[Mod],
      body: Term
  )(implicit ev: Extract[D, Mod], ctx: SemanticDocument): Boolean = {
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
      body.is[Lit] && skipSimpleDefinitions

    def isImplicit: Boolean =
      defn.hasMod(mod"implicit") && !isImplicitly(body)

    def hasParentWihTemplate: Boolean =
      defn.parent.exists(_.is[Template])

    def isLocal: Boolean =
      if (config.skipLocalImplicits) nm.symbol.isLocal
      else false

    isImplicit && !isFinalLiteralVal && !isLocal || {
      hasParentWihTemplate &&
      !defn.hasMod(mod"implicit") &&
      !matchesSimpleDefinition() &&
      matchesMemberKind() &&
      matchesMemberVisibility()
    }
  }

  def defnType(defn: Defn, replace: Token, space: String, types: TypeRewrite)(
      implicit ctx: SemanticDocument
  ): Option[Patch] =
    for {
      name <- defnName(defn)
      defnSymbol <- name.symbol.asNonEmpty
      patch <- types.toPatch(name.pos, defnSymbol, replace, space)
    } yield patch

  def fixDefinition(defn: Defn, body: Term, types: TypeRewrite)(
      implicit ctx: SemanticDocument
  ): Patch = {
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
      space = {
        if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
        else ""
      }
      patch <- defnType(defn, replace, space, types)
    } yield patch
  }.asPatch.atomic

}
