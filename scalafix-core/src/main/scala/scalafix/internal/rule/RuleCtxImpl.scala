package scalafix.internal.rule

import scala.meta._
import scala.meta.internal.inputs.XtensionInput
import scala.meta.tokens.Tokens

import org.scalameta.FileLine
import org.scalameta.logger
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch
import scalafix.internal.patch.LegacyPatchOps
import scalafix.internal.v1.LazyValue
import scalafix.syntax._
import scalafix.util.MatchingParens
import scalafix.util.SemanticdbIndex
import scalafix.util.TokenList
import scalafix.v0._

class RuleCtxImpl(
    val tree: Tree,
    val config: ScalafixConfig,
    diffDisable: DiffDisable
) extends RuleCtx
    with LegacyPatchOps { ctx =>
  def syntax: String =
    s"""${tree.input.syntax}
      |${logger.revealWhitespace(tree.syntax.take(100))}""".stripMargin
  override def toString: String = syntax
  override def tokens: Tokens = tree.tokens
  lazy val tokenList: TokenList = TokenList(tokens)
  lazy val matchingParens: MatchingParens = MatchingParens(tokens)
  lazy val input: Input = tokens.head.input
  lazy val escapeHatch: EscapeHatch =
    EscapeHatch(
      input,
      LazyValue.now(tree),
      diffDisable
    )

  // Debug utilities
  def index(implicit index: SemanticdbIndex): SemanticdbIndex =
    index
  def debugIndex()(implicit
      index: SemanticdbIndex,
      fileLine: FileLine
  ): Unit = {
    val db = this.index(index)
    debug(sourcecode.Text(db.documents.head, "index"))
  }
  def debug(
      values: sourcecode.Text[Any]*
  )(implicit fileLine: FileLine): Unit = {
    // alias for org.scalameta.logger.
    logger.elem(values: _*)
  }

}
