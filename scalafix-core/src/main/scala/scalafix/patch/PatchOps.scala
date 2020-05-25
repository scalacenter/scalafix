package scalafix.patch

import scala.meta._
import scalafix.lint.Diagnostic
import scalafix.util.SemanticdbIndex
import scalafix.v0.Symbol

trait PatchOps {

  /**
   * Remove this particular instance of Importee.
   *
   * Handles tricky cases like trailing commas or curly braces.
   * Example, removeImportee(b) in `import a.{b, c}` produces
   * `import a.c`.
   *
   * Note, `importee` instance is by reference, so removing quasiquotes
   * (example, `removeImportee(importee"b")`) does nothing.
   */
  def removeImportee(importee: Importee): Patch

  /**
   * Add this importer to the global imports at the top of this file.
   *
   * It is OK to pass in a quasiquote here. The importer has no attached
   * semantic information, so it's not possible to deduplicate Importers.
   */
  def addGlobalImport(importer: Importer): Patch

  /** Replace the entire contents of this Token with toReplace. */
  def replaceToken(token: Token, toReplace: String): Patch

  /** Replace all tokens with empty string. */
  def removeTokens(tokens: Tokens): Patch
  def removeTokens(tokens: Iterable[Token]): Patch

  /** Replace single token with empty string. */
  def removeToken(token: Token): Patch

  /** Replace all tokens of tree contents with toReplace. */
  def replaceTree(tree: Tree, toReplace: String): Patch

  /** Add the string toAdd to the right side of token, while keeping token. */
  def addRight(token: Token, toAdd: String): Patch

  /** Add the string toAdd to the left side of token, while keeping token. */
  def addLeft(token: Token, toAdd: String): Patch

  /** Add the string toAdd to the last token of tree. Does not remove tokens. */
  def addRight(tree: Tree, toAdd: String): Patch

  /** Add the string toAdd to the first token of tree. Does not remove tokens. */
  def addLeft(tree: Tree, toAdd: String): Patch

  // Semantic patches below, available to rules that "extends SemanticRule"

  /**
   * Report a linter message.
   *
   * To construct a LintMessage, start by creating a lint category. Example:
   *
   * {{{
   *   class MyLinter extends Rule {
   *     val divisionByZero = scalafix.LintCategory.error("Division by zero!")
   *     val divisionTree: scala.meta.Tree = ???
   *     PatchOps.lint(divisionByZero.at(divisionTree.pos))
   *   }
   * }}}
   *
   * Each LintCategory is assigned a unique identifier, which is formatted
   * as "RuleName.categoryID". The divisionByZero example would have the
   * id "MyLinter.divisionByZero". A LintCategory has a default severity level
   * (warning, error) that the user can override in .scalafix.conf.
   */
  def lint(msg: Diagnostic): Patch

  /**
   * Remove importees that resolve to symbol.
   *
   * Note, this patch is not reference, unlike removeImportee(Importee). It is
   * only necessary to use this patch once per tree, duplicate symbols are
   * ignored.
   */
  def removeGlobalImport(symbol: Symbol)(implicit index: SemanticdbIndex): Patch

  /**
   * Add an import on symbol among the global imports.
   *
   * This patch is not reference, unlike removeImportee(Importee). It is
   * only necessary to use this patch once per tree, duplicate symbols are
   * ignored.
   */
  def addGlobalImport(symbol: Symbol)(implicit index: SemanticdbIndex): Patch

  /**
   * Replace references/call-sites to fromSymbol with references to toSymbol.
   *
   * toSymbol must be a static method or a globally accessible object.
   * toSymbol should not be path dependent. To rename a class method,
   * use renameSymbol.
   *
   * Experimental. May produce broken code in some cases. This
   * is the same patch as `replace:com.foo/com.bar` from sbt-scalafix.
   */
  def replaceSymbol(fromSymbol: Symbol.Global, toSymbol: Symbol.Global)(
      implicit index: SemanticdbIndex
  ): Patch

  /**
   * Replace appearances of names that reference fromSymbol with toName.
   *
   * toName should be a legal identifier, it cannot be a tree such as `foo()`.
   * Use this patch for example to rename a methods on a class.
   */
  def renameSymbol(fromSymbol: Symbol.Global, toName: String)(
      implicit index: SemanticdbIndex
  ): Patch

  /**
   * Shorthand for calling replaceSymbol from strings.
   *
   * String values are treated as Symbol.Global.
   */
  def replaceSymbols(toReplace: (String, String)*)(
      implicit index: SemanticdbIndex
  ): Patch

  /**
   * Helper for calling replaceSymbols without needing _*
   *
   * Defers work to replaceSymbols((String, String)*). Needs a dummy implicit
   * to differentiate from replaceSymbols((String, String)*) after type erasure
   */
  def replaceSymbols(
      toReplace: scala.collection.Seq[(String, String)]
  )(implicit noop: DummyImplicit, index: SemanticdbIndex): Patch
}
