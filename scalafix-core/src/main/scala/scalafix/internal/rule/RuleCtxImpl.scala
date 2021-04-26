package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens

import org.scalameta.FileLine
import org.scalameta.logger
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.patch.EscapeHatch
import scalafix.internal.patch.LegacyPatchOps
import scalafix.syntax._
import scalafix.util.MatchingParens
import scalafix.util.SemanticdbIndex
import scalafix.util.TokenList
import scalafix.v0._
import scalafix.v1.SyntacticDocument

class RuleCtxImpl(
    val doc: SyntacticDocument,
    val config: ScalafixConfig
) extends RuleCtx
    with LegacyPatchOps { ctx =>
  def syntax: String =
    s"""${tree.input.syntax}
      |${logger.revealWhitespace(tree.syntax.take(100))}""".stripMargin
  override def toString: String = syntax
  def toks(t: Tree): Tokens = t.tokens(config.dialect)
  lazy val tree = doc.tree
  lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = TokenList(tokens)
  lazy val matchingParens: MatchingParens = MatchingParens(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  lazy val input: Input = tokens.head.input
  lazy val escapeHatch: EscapeHatch = doc.internal.escapeHatch.value

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
