package scalafix
package rule

import scala.collection.immutable.Seq
import scala.meta._
import scalafix.internal.config.MetaconfigPendingUpstream
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.internal.config.ScalafixConfig
import scalafix.syntax._
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.Configured

/** A Rule is a program that produces a Patch from a scala.meta.Tree. */
abstract class Rule { self =>

  /** Name of this rule.
    *
    * By convention, this name should be PascalCase matching the class name
    * of the rule.
    *
    * Example good name: NoVars, ExplicitUnit.
    * Example bad name: no-vars, noVars, FixVars.
    */
  def name: RuleName // = RuleName(this.getClass.getSimpleName)

  /** Returns linter messages to report violations of this rule. */
  def check(ctx: RuleCtx): List[LintMessage] = Nil

  /** Returns a patch to fix violations of this rule. */
  def fix(ctx: RuleCtx): Patch = Patch.empty

  /** Initialize rule.
    *
    * This method is called once by scalafix before rule is called.
    * Use this method to either read custom configuration or to build
    * expensive indices.
    *
    * @param config The .scalafix.conf configuration.
    * @return the initialized rule or an error. If no initialization is needed,
    *         return Configured.Ok(this).
    */
  def init(config: Conf): Configured[Rule] =
    Configured.Ok(this)

  /** Combine this rule with another rule. */
  final def merge(other: Rule): Rule = Rule.merge(this, other)
  @deprecated("Renamed to merge.", "0.5.0")
  final def andThen(other: Rule): Rule = merge(other)

  /** Returns string output of applying this single patch. */
  final def apply(ctx: RuleCtx): String = apply(ctx, fix(ctx))
  final def apply(
      input: Input,
      config: ScalafixConfig = ScalafixConfig.default): String = {
    val ctx = RuleCtx(config.dialect(input).parse[Source].get, config)
    val patch = fix(ctx)
    apply(ctx, patch)
  }
  final def apply(input: String): String = apply(Input.String(input))
  final def apply(ctx: RuleCtx, patch: Patch): String = {
    val result = Patch(patch, ctx, semanticOption)
    Patch.lintMessages(patch, ctx).foreach { msg =>
      // Set the lint message owner. This allows us to distinguish
      // LintCategory with the same id from different rules.
      ctx.printLintMessage(msg, name)
    }
    result
  }

  /** Returns unified diff from applying this patch */
  final def diff(ctx: RuleCtx): String =
    diff(ctx, fix(ctx))
  final protected def diff(ctx: RuleCtx, patch: Patch): String = {
    val original = ctx.tree.input
    Patch.unifiedDiff(
      original,
      Input.VirtualFile(original.label, apply(ctx, patch)))
  }

  private[scalafix] final def allNames: List[String] =
    name.identifiers.map(_.value)
  final override def toString: String = name.toString

  // NOTE. This is kind of hacky and hopefully we can find a better workaround.
  // The challenge is the following:
  // - a.andThen(b) needs to work for mixing semantic + syntactic rules.
  // - applied/appliedDiff should work without passing in SemanticCtx explicitly
  protected[scalafix] def semanticOption: Option[SemanticCtx] = None
}

abstract class SemanticRule(sctx: SemanticCtx) extends Rule {
  implicit val ImplicitSemanticCtx: SemanticCtx = sctx
  override def semanticOption: Option[SemanticCtx] = Some(sctx)
}

object Rule {
  private[scalafix] class CompositeRule(val rules: List[Rule]) extends Rule {
    override def name: RuleName = rules.foldLeft(RuleName.empty) {
      case (a, rule) => a + rule.name
    }
    override def init(config: Conf): Configured[Rule] = {
      MetaconfigPendingUpstream
        .flipSeq(rules.map(_.init(config)))
        .map(x => new CompositeRule(x.toList))
    }
    override def fix(ctx: RuleCtx): Patch =
      Patch.empty ++ rules.map(_.fix(ctx))
    override def semanticOption: Option[SemanticCtx] =
      rules
        .collectFirst {
          case r if r.semanticOption.isDefined => r.semanticOption
        }
        .getOrElse(None)
  }
  val syntaxRuleConfDecoder: ConfDecoder[Rule] =
    ScalafixMetaconfigReaders.ruleConfDecoderSyntactic(
      ScalafixMetaconfigReaders.baseSyntacticRuleDecoder)
  lazy val empty: Rule = new Rule { def name: RuleName = RuleName.empty }
  def emptyConfigured: Configured[Rule] = Configured.Ok(empty)
  def emptyFromSemanticCtxOpt(sctx: Option[SemanticCtx]): Rule =
    sctx.fold(empty)(emptySemantic)
  def combine(rules: Seq[Rule]): Rule =
    rules.foldLeft(empty)(_ merge _)
  private[scalafix] def emptySemantic(sctx: SemanticCtx): Rule =
    semantic(RuleName.empty.value)(_ => _ => Patch.empty)(sctx)

  /** Creates a linter. */
  def linter(ruleName: String)(f: RuleCtx => List[LintMessage]): Rule =
    new Rule {
      override def name: RuleName = ruleName
      override def check(ctx: RuleCtx): List[LintMessage] = f(ctx)
    }

  /** Creates a syntactic rule. */
  def syntactic(ruleName: String)(f: RuleCtx => Patch): Rule =
    new Rule {
      override def name: RuleName = ruleName
      override def fix(ctx: RuleCtx): Patch = f(ctx)
    }

  /** Creates a semantic rule. */
  def semantic(ruleName: String)(
      f: SemanticCtx => RuleCtx => Patch): SemanticCtx => Rule = { sctx =>
    new SemanticRule(sctx) {
      override def name: RuleName = ruleName
      override def fix(ctx: RuleCtx): Patch = f(sctx)(ctx)
    }
  }

  /** Creates a rule that always returns the same patch. */
  def constant(ruleName: String, patch: Patch, sctx: SemanticCtx): Rule =
    new SemanticRule(sctx) {
      override def name: RuleName = ruleName
      override def fix(ctx: RuleCtx): Patch = patch
    }

  /** Combine two rules into a single rule */
  def merge(a: Rule, b: Rule): Rule =
    new CompositeRule(a :: b :: Nil)
}
