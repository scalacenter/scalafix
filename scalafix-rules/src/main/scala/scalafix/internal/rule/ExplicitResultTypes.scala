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
import scala.util.Properties
import scalafix.internal.compat.CompilerCompat._

final class ExplicitResultTypes(
    config: ExplicitResultTypesConfig,
    global: LazyValue[Option[ScalafixGlobal]]
) extends SemanticRule("ExplicitResultTypes") {

  def this() = this(ExplicitResultTypesConfig.default, LazyValue.now(None))

  private def supportedScalaVersion = Properties.versionNumberString
  override def description: String =
    "Inserts type annotations for inferred public members. " +
      s"Only compatible with Scala $supportedScalaVersion."
  override def isRewrite: Boolean = true

  override def afterComplete(): Unit = {
    shutdownCompiler()
  }

  private def shutdownCompiler(): Unit = {
    global.foreach(_.foreach(g => {
      try {
        g.askShutdown()
        g.closeCompat()
      } catch {
        case NonFatal(_) =>
      }
    }))
  }

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val symbolReplacements =
      config.conf.dynamic.ExplicitResultTypes.symbolReplacements
        .as[Map[String, String]]
        .getOrElse(Map.empty)
    val newGlobal: LazyValue[Option[ScalafixGlobal]] =
      if (config.scalacClasspath.isEmpty) {
        LazyValue.now(None)
      } else {
        LazyValue.fromUnsafe { () =>
          ScalafixGlobal.newCompiler(
            config.scalacClasspath,
            config.scalacOptions,
            symbolReplacements
          )
        }
      }
    if (config.scalacClasspath.nonEmpty && config.scalaVersion != supportedScalaVersion) {
      Configured.error(
        s"The ExplicitResultTypes rule only supports the Scala version '$supportedScalaVersion'. " +
          s"To fix this problem, either remove `ExplicitResultTypes` from .scalafix.conf or change the Scala version " +
          s"in your build to match exactly '$supportedScalaVersion'."
      )
    } else {
      config.conf // Support deprecated explicitReturnTypes config
        .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
          ExplicitResultTypesConfig.default
        )
        .map(c => new ExplicitResultTypes(c, newGlobal))
    }
  }

  override def fix(implicit ctx: SemanticDocument): Patch = {
    try unsafeFix()
    catch {
      case _: CompilerException =>
        shutdownCompiler()
        global.restart()
        try unsafeFix()
        catch {
          case _: CompilerException if !config.fatalWarnings =>
            // Ignore compiler crashes unless `fatalWarnings = true`.
            Patch.empty
        }
    }
  }
  def unsafeFix()(implicit ctx: SemanticDocument): Patch = {
    lazy val types = TypePrinter(global.value, config)
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

  def defnBody(defn: Defn): Option[Term] = Option(defn).collect {
    case Defn.Val(_, _, _, term) => term
    case Defn.Var(_, _, _, Some(term)) => term
    case Defn.Def(_, _, _, _, _, term) => term
  }

  def visibility(mods: Iterable[Mod]): MemberVisibility =
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
      mods: Iterable[Mod],
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
      config.skipSimpleDefinitions.isSimpleDefinition(body)

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

  def defnType(defn: Defn, replace: Token, space: String, types: TypePrinter)(
      implicit ctx: SemanticDocument
  ): Option[Patch] =
    for {
      name <- defnName(defn)
      defnSymbol <- name.symbol.asNonEmpty
      patch <- types.toPatch(name.pos, defnSymbol, replace, defn, space)
    } yield patch

  def fixDefinition(defn: Defn, body: Term, types: TypePrinter)(
      implicit ctx: SemanticDocument
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
        case q"${option(_)}.empty[$_]" =>
          Patch.replaceTree(term, "None")
        case q"${list(_)}.empty[$_]" =>
          Patch.replaceTree(term, "Nil")
        case q"${seq(_)}.empty[$_]" =>
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
      typePatch <- defnType(defn, replace, space, types)
      valuePatchOpt = defnBody(defn).map(patchEmptyValue)
    } yield typePatch + valuePatchOpt
  }.asPatch.atomic

}
