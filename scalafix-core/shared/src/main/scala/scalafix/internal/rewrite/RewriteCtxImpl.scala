package scalafix.internal.rewrite

import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.tokens.Tokens
import scalafix.LintMessage
import scalafix.syntax._
import scalafix._
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.internal.config.ScalafixReporter
import scalafix.internal.util.SymbolOps.BottomSymbol
import scalafix.patch.LintPatch
import scalafix.patch.TokenPatch
import scalafix.patch.TokenPatch.Add
import scalafix.patch.TreePatch
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.RemoveGlobalImport
import scalafix.rewrite.RewriteCtx
import scalafix.rewrite.RewriteName
import scalafix.util.MatchingParens
import scalafix.util.SemanticCtx
import scalafix.util.TokenList
import org.scalameta.FileLine
import org.scalameta.logger

case class RewriteCtxImpl(tree: Tree, config: ScalafixConfig)
    extends RewriteCtx {
  ctx =>
  def syntax: String =
    s"""${tree.input.syntax}
       |${logger.revealWhitespace(tree.syntax.take(100))}""".stripMargin
  override def toString: String = syntax
  def toks(t: Tree): Tokens = t.tokens(config.dialect)
  implicit lazy val tokens: Tokens = tree.tokens(config.dialect)
  lazy val tokenList: TokenList = new TokenList(tokens)
  lazy val matching: MatchingParens = MatchingParens(tokens)
  lazy val comments: AssociatedComments = AssociatedComments(tokens)
  lazy val input: Input = tokens.head.input
  val reporter: ScalafixReporter = config.reporter

  // Debug utilities
  def sctx(implicit sctx: SemanticCtx): SemanticCtx =
    SemanticCtx(sctx.entries.filter(_.input == input))
  def debugSemanticCtx()(implicit sctx: SemanticCtx, fileLine: FileLine): Unit = {
    val db = this.sctx(sctx)
    debug(sourcecode.Text(db, "sctx"))
  }
  def debug(values: sourcecode.Text[Any]*)(implicit fileLine: FileLine): Unit = {
    // alias for org.scalameta.logger.
    logger.elem(values: _*)
  }

  def printLintMessage(msg: LintMessage, owner: RewriteName): Unit = {
    val key = msg.category.key(owner)
    if (config.lint.ignore.matches(key)) ()
    else {
      val category = config.lint
        .getConfiguredSeverity(key)
        .getOrElse(msg.category.severity)
      config.lint.reporter.handleMessage(
        msg.format(owner, config.lint.explain),
        msg.position,
        category.toSeverity
      )
    }
  }

  def lint(msg: LintMessage): Patch =
    LintPatch(msg)

  // Syntactic patch ops.
  def removeImportee(importee: Importee): Patch =
    TreePatch.RemoveImportee(importee)
  def replaceToken(token: Token, toReplace: String): Patch =
    Add(token, "", toReplace, keepTok = false)
  def removeTokens(tokens: Tokens): Patch =
    tokens.foldLeft(Patch.empty)(_ + TokenPatch.Remove(_))
  def removeToken(token: Token): Patch = Add(token, "", "", keepTok = false)
  def replaceTree(from: Tree, to: String): Patch = {
    val tokens = toks(from)
    removeTokens(tokens) + tokens.headOption.map(x => addRight(x, to))
  }
  def rename(from: Name, to: Name): Patch =
    rename(from, to.value)
  def rename(from: Name, to: String): Patch =
    if (from.value == to) Patch.empty
    else
      ctx
        .toks(from)
        .headOption
        .fold(Patch.empty)(tok => Add(tok, "", to, keepTok = false))
  def addRight(tok: Token, toAdd: String): Patch = Add(tok, "", toAdd)
  def addRight(tree: Tree, toAdd: String): Patch =
    toks(tree).lastOption.fold(Patch.empty)(addRight(_, toAdd))
  def addLeft(tok: Token, toAdd: String): Patch = Add(tok, toAdd, "")
  def addLeft(tree: Tree, toAdd: String): Patch =
    toks(tree).lastOption.fold(Patch.empty)(addLeft(_, toAdd))

  // Semantic patch ops.
  def removeGlobalImport(symbol: Symbol)(implicit sctx: SemanticCtx): Patch =
    RemoveGlobalImport(symbol)
  def addGlobalImport(symbol: Symbol)(implicit sctx: SemanticCtx): Patch =
    TreePatch.AddGlobalSymbol(symbol)
  def addGlobalImport(importer: Importer)(implicit sctx: SemanticCtx): Patch =
    AddGlobalImport(importer)
  def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit sctx: SemanticCtx): Patch =
    TreePatch.ReplaceSymbol(fromSymbol, toSymbol)
  def replaceSymbols(toReplace: (String, String)*)(
      implicit sctx: SemanticCtx): Patch =
    toReplace.foldLeft(Patch.empty) {
      case (a, (from, to)) =>
        val (fromSymbol, toSymbol) =
          ScalafixMetaconfigReaders.parseReplaceSymbol(from, to).get
        a + ctx.replaceSymbol(fromSymbol, toSymbol)
    }
  def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit sctx: SemanticCtx): Patch =
    TreePatch.ReplaceSymbol(
      fromSymbol,
      Symbol.Global(BottomSymbol(), Signature.Term(toName)))

}
