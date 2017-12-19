package scalafix.internal.rule

import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.LintMessage
import scalafix._
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch
import scalafix.internal.util.SymbolOps.Root
import scalafix.patch.LintPatch
import scalafix.patch.TokenPatch
import scalafix.patch.TokenPatch.Add
import scalafix.patch.TreePatch
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.RemoveGlobalImport
import scalafix.rule.RuleCtx
import scalafix.rule.RuleName
import scalafix.syntax._
import scalafix.util.MatchingParens
import scalafix.util.SemanticdbIndex
import scalafix.util.TokenList

import org.scalameta.FileLine
import org.scalameta.logger

case class RuleCtxImpl(
    tree: Tree,
    config: ScalafixConfig,
    diffDisable: DiffDisable)
    extends RuleCtx {
  ctx =>
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

  def lint(msg: LintMessage): Patch =
    LintPatch(msg)

  // Syntactic patch ops.
  def removeImportee(importee: Importee): Patch =
    TreePatch.RemoveImportee(importee)
  def addGlobalImport(importer: Importer): Patch =
    AddGlobalImport(importer)
  def replaceToken(token: Token, toReplace: String): Patch =
    Add(token, "", toReplace, keepTok = false)
  def removeTokens(tokens: Tokens): Patch =
    doRemoveTokens(tokens)
  def removeTokens(tokens: Iterable[Token]): Patch =
    doRemoveTokens(tokens)
  private def doRemoveTokens(tokens: Iterable[Token]): Patch =
    tokens.foldLeft(Patch.empty)(_ + TokenPatch.Remove(_))
  def removeToken(token: Token): Patch = Add(token, "", "", keepTok = false)
  def replaceTree(from: Tree, to: String): Patch = {
    val tokens = toks(from)
    removeTokens(tokens) + tokens.headOption.map(x => addRight(x, to))
  }
  def addRight(tok: Token, toAdd: String): Patch = Add(tok, "", toAdd)
  def addRight(tree: Tree, toAdd: String): Patch =
    toks(tree).lastOption.fold(Patch.empty)(addRight(_, toAdd))
  def addLeft(tok: Token, toAdd: String): Patch = Add(tok, toAdd, "")
  def addLeft(tree: Tree, toAdd: String): Patch =
    toks(tree).headOption.fold(Patch.empty)(addLeft(_, toAdd))

  // Semantic patch ops.
  def removeGlobalImport(symbol: Symbol)(
      implicit index: SemanticdbIndex): Patch =
    RemoveGlobalImport(symbol)
  def addGlobalImport(symbol: Symbol)(implicit index: SemanticdbIndex): Patch =
    TreePatch.AddGlobalSymbol(symbol)
  def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit index: SemanticdbIndex): Patch =
    TreePatch.ReplaceSymbol(fromSymbol, toSymbol)
  def replaceSymbols(toReplace: (String, String)*)(
      implicit index: SemanticdbIndex): Patch =
    toReplace.foldLeft(Patch.empty) {
      case (a, (from, to)) =>
        val (fromSymbol, toSymbol) =
          ScalafixMetaconfigReaders.parseReplaceSymbol(from, to).get
        a + ctx.replaceSymbol(fromSymbol, toSymbol)
    }
  def replaceSymbols(toReplace: Seq[(String, String)])(
      implicit noop: DummyImplicit,
      index: SemanticdbIndex): Patch = {
    replaceSymbols(toReplace: _*)
  }
  def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit index: SemanticdbIndex): Patch =
    TreePatch.ReplaceSymbol(fromSymbol, Root(Signature.Term(toName)))

}
