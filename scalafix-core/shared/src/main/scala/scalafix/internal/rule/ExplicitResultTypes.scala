package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.scalafix.ScalafixScalametaHacks
import scalafix.Patch
import scalafix.SemanticdbIndex
import scalafix.internal.config.ExplicitResultTypesConfig
import scalafix.internal.config.MemberKind
import scalafix.internal.config.MemberVisibility
import scalafix.rule.Rule
import scalafix.rule.RuleCtx
import scalafix.rule.RuleName
import scalafix.rule.SemanticRule
import scalafix.syntax._
import scalafix.util.TokenOps
import metaconfig.Conf
import metaconfig.Configured
import org.langmeta.internal.semanticdb.XtensionDenotationsInternal
import scala.util.control.NonFatal
import scalafix.internal.util.EagerInMemorySemanticdbIndex
import scalafix.internal.util.Shorten
import scalafix.internal.util.TypeToTree

case class ExplicitResultTypes(
    index: SemanticdbIndex,
    config: ExplicitResultTypesConfig = ExplicitResultTypesConfig.default)
    extends SemanticRule(
      index,
      RuleName("ExplicitResultTypes")
        .withDeprecatedName(
          "ExplicitReturnTypes",
          "Renamed to ExplicitResultTypes",
          "0.5")
    ) {

  override def description: String =
    "Rewrite that inserts explicit type annotations for def/val/var"

  def this(index: SemanticdbIndex) =
    this(index, ExplicitResultTypesConfig.default)
  override def init(config: Conf): Configured[Rule] =
    config // Support deprecated explicitReturnTypes config
      .getOrElse("explicitReturnTypes", "ExplicitResultTypes")(
        ExplicitResultTypesConfig.default)
      .map(c => ExplicitResultTypes(index, c))

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

  override def fix(ctx: RuleCtx): Patch = {
    val table = index.asInstanceOf[EagerInMemorySemanticdbIndex]
    val pretty = new TypeToTree(
      table,
      if (config.unsafeShortenNames) Shorten.Readable
      else Shorten.FullyQualified)
    def defnType(defn: Defn): Option[(Type, Patch)] =
      for {
        name <- defnName(defn)
        sym <- name.symbol
        info <- table.info(sym.syntax)
        result <- {
          try {
            import scala.meta.internal.{semanticdb3 => s}
            pretty.toTree(info)
            val tpe = info.tpe.get.tag match {
              case s.Type.Tag.METHOD_TYPE =>
                info.tpe.get.methodType.get.returnType.get
              case _ => info.tpe.get
            }
            Some(pretty.toType(tpe))
          } catch {
            case NonFatal(e) =>
              e.setStackTrace(e.getStackTrace.take(30))
              e.printStackTrace()
              None
          }
        }
      } yield result -> Patch.empty
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
        replace <- lhsTokens.reverseIterator.find(x =>
          !x.is[Token.Equals] && !x.is[Trivia])
        (typ, patch) <- defnType(defn)
        space = {
          if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
          else ""
        }
      } yield ctx.addRight(replace, s"$space: ${treeSyntax(typ)}") + patch
    }.asPatch.atomic

    def treeSyntax(tree: Tree): String =
      ScalafixScalametaHacks.resetOrigin(tree).syntax

    def isRuleCandidate[D <: Defn](
        defn: D,
        nm: Name,
        mods: Traversable[Mod],
        body: Term)(implicit ev: Extract[D, Mod]): Boolean = {
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

      def isLocal =
        if (config.skipLocalImplicits) nm.symbol match {
          case Some(value) => value.isInstanceOf[scala.meta.Symbol.Local]
          case None => false
        } else false

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
