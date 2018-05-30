package scalafix.internal.rule

import org.scalameta.FileLine
import org.scalameta.logger
import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.v0._
import scalafix.syntax._
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.DeprecatedPatchOps
import scalafix.internal.patch.EscapeHatch
import scalafix.rule.RuleName
import scalafix.util.MatchingParens
import scalafix.util.SemanticdbIndex
import scalafix.util.TokenList

case class RuleCtxImpl(
    tree: Tree,
    config: ScalafixConfig,
    diffDisable: DiffDisable)
    extends RuleCtx
    with DeprecatedPatchOps { ctx =>
  def syntax: String =
    s"""${tree.input.syntax}
       |${logger.revealWhitespace(tree.syntax.take(100))}""".stripMargin
  override def toString: String = syntax
  def toks(t: Tree): Tokens = t.tokens(config.dialect)
  lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = TokenList(tokens)
  lazy val matchingParens: MatchingParens = MatchingParens(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  lazy val input: Input = tokens.head.input
  lazy val escapeHatch: EscapeHatch = EscapeHatch(tree, comments)

  // Debug utilities
  def index(implicit index: SemanticdbIndex): SemanticdbIndex =
    index.withDocuments(index.documents.filter(_.input == input))
  def debugIndex()(
      implicit index: SemanticdbIndex,
      fileLine: FileLine): Unit = {
    val db = this.index(index)
    debug(sourcecode.Text(db.documents.head, "index"))
  }
  def debug(values: sourcecode.Text[Any]*)(
      implicit fileLine: FileLine): Unit = {
    // alias for org.scalameta.logger.
    logger.elem(values: _*)
  }

  def printLintMessage(msg: LintMessage): Unit = {
    val category = msg.category.withConfig(config.lint)

    config.lint.reporter.handleMessage(
      msg.format(config.lint.explain),
      msg.position,
      category.severity.toSeverity
    )
  }

  def filter(
      patchesByName: Map[RuleName, Patch],
      index: SemanticdbIndex): (Patch, List[LintMessage]) =
    escapeHatch.filter(patchesByName, this, index, diffDisable)

}
