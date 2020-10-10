package scalafix.patch

import scala.meta.Token
import scala.meta._

import org.scalameta.logger
import scalafix.internal.config.ScalafixMetaconfigReaders
import scalafix.internal.util.SymbolOps.Root
import scalafix.lint.Diagnostic
import scalafix.patch.Patch.internal._
import scalafix.v0
import scalafix.v1
import scalafix.v1.SemanticContext

/**
 * An immutable sealed data structure that describes how to rewrite and lint a source file.
 *
 * Documentation: https://scalacenter.github.io/scalafix/docs/developers/patch.html
 */
sealed abstract class Patch extends Product {

  /** Merge this patch together with the other patch. */
  def +(other: Patch): Patch =
    if (this eq other) this
    else if (isEmpty) other
    else if (other.isEmpty) this
    else Concat(this, other)

  /** Merge this patch together with the other patch if non-empty. */
  def +(other: Option[Patch]): Patch =
    this.+(other.getOrElse(Patch.empty))

  /** Merge this patch together with the other patches. */
  def ++(other: Iterable[Patch]): Patch = other.foldLeft(this)(_ + _)

  /**
   * Returns true if this patch is equal to Patch.empty, false otherwise.
   *
   * Note, may return false for patches that produce empty diffs. For example, a [[Patch]].replaceSymbol
   * returns false even if the symbol from is not referenced in the code resulting in an empty diff.
   */
  def isEmpty: Boolean = this == EmptyPatch

  /**
   * @see [[Patch.isEmpty]]
   */
  def nonEmpty: Boolean = !isEmpty

  /** Skip this entire patch if a part of it is disabled with // scalafix:off */
  def atomic: Patch = if (isEmpty) this else AtomicPatch(this)
}

object Patch {

  /** Combine a sequence of patches into a single patch. */
  def fromIterable(seq: Iterable[Patch]): Patch =
    seq.foldLeft(empty)(_ + _)

  /** Do nothing: no diff, no diagnostics. */
  val empty: Patch = EmptyPatch

  /** Reports error/warning/info message at a position. */
  def lint(msg: Diagnostic): Patch =
    LintPatch(msg)

  /** Remove this importee reference. */
  def removeImportee(importee: Importee): Patch =
    RemoveImportee(importee)
  /** Add this importer to the global import list. */
  def addGlobalImport(importer: Importer): Patch =
    AddGlobalImport(importer)
  /** Remove the token and insert this string at the same position. */
  def replaceToken(token: Token, toReplace: String): Patch =
    Add(token, "", toReplace, keepTok = false)
  /** Remove all of the these tokens from the source file. */
  def removeTokens(tokens: Iterable[Token]): Patch =
    tokens.foldLeft(Patch.empty)(_ + Remove(_))
  /** Remove this single token from the source file. */
  def removeToken(token: Token): Patch =
    Add(token, "", "", keepTok = false)

  /**
   * Remove all tokens from this tree and add a string add the same position.
   *
   * Beware that this patch does not compose with other patches touching the same
   * tree node or its children. Avoid using this method for large tree nodes like
   * classes of methods. It's recommended to target as precise tree nodes as possible.
   *
   * It is better to use addRight/addLeft if you only insert new code, example:
   * - bad:  Patch.replaceTree(tree, "(" + tree.syntax + ")")
   * - good: Patch.addLeft(tree, "(") + Patch.addRight(tree, + ")")
   */
  def replaceTree(from: Tree, to: String): Patch = {
    if (from.pos == Position.None) Patch.empty
    else {
      removeTokens(from.tokens) +
        from.tokens.headOption.map(x => addRight(x, to))
    }
  }
  /** Add this string to the right of this token. */
  def addRight(tok: Token, toAdd: String): Patch =
    Add(tok, "", toAdd)
  /** Add this string to the right of this tree. */
  def addRight(tree: Tree, toAdd: String): Patch =
    tree.tokens.lastOption.fold(Patch.empty)(addRight(_, toAdd))
  /** Add this string to the left of this token. */
  def addLeft(tok: Token, toAdd: String): Patch =
    Add(tok, toAdd, "")
  /** Add this string to the left of this tree. */
  def addLeft(tree: Tree, toAdd: String): Patch =
    tree.tokens.headOption.fold(Patch.empty)(addLeft(_, toAdd))

  /**
   * Remove named imports for this symbol.
   *
   * Does not remove wildcard imports for the enclosing package or class.
   */
  def removeGlobalImport(
      symbol: v1.Symbol
  )(implicit c: SemanticContext): Patch =
    RemoveGlobalImport(v0.Symbol(symbol.value))
  /** Place named import for this symbol at the bottom of the global import list */
  def addGlobalImport(symbol: v1.Symbol)(implicit c: SemanticContext): Patch =
    AddGlobalSymbol(v0.Symbol(symbol.value))

  /**
   * Replace occurrences of fromSymbol to reference toSymbol instead.
   *
   * `toSymbol` must be a global symbol such as an object/class or a static method.
   *
   * May produce broken code in some cases, works best when toSymbol has the same depth
   * as fromSymbol, example:
   * - good: replace:com.foo.Bar/org.qux.Buz
   * - bad:  replace:com.Bar/org.qux.Buz
   */
  def replaceSymbols(
      toReplace: (String, String)*
  )(implicit c: SemanticContext): Patch =
    toReplace.foldLeft(Patch.empty) {
      case (a, (from, to)) =>
        val (fromSymbol, toSymbol) =
          ScalafixMetaconfigReaders.parseReplaceSymbol(from, to).get
        a + ReplaceSymbol(fromSymbol, toSymbol)
    }

  /** Replace occurrences of fromSymbol to use toName instead */
  def renameSymbol(fromSymbol: v1.Symbol, toName: String)(
      implicit c: SemanticContext
  ): Patch =
    ReplaceSymbol(
      v0.Symbol(fromSymbol.value).asInstanceOf[v0.Symbol.Global],
      Root(v0.Signature.Term(toName))
    )

  private[scalafix] object internal {
    trait LowLevelPatch
    abstract class TokenPatch(val tok: Token, val newTok: String)
        extends Patch
        with LowLevelPatch {
      override def toString: String =
        if (newTok.isEmpty)
          s"Remove(${logger.revealWhitespace(tok.structure)})"
        else
          s"$productPrefix(${logger.revealWhitespace(tok.syntax)}, ${tok.structure}, $newTok)"
    }
    case class Remove(override val tok: Token) extends TokenPatch(tok, "")
    case class Add(
        override val tok: Token,
        addLeft: String,
        addRight: String,
        keepTok: Boolean = true
    ) extends TokenPatch(
          tok,
          s"""$addLeft${if (keepTok) tok else ""}$addRight"""
        )
    abstract class TreePatch extends Patch
    abstract class ImportPatch extends TreePatch
    case class RemoveGlobalImport(symbol: v0.Symbol) extends ImportPatch
    case class RemoveImportee(importee: Importee) extends ImportPatch
    case class AddGlobalImport(importer: Importer) extends ImportPatch
    case class AddGlobalSymbol(symbol: v0.Symbol) extends ImportPatch
    case class ReplaceSymbol(from: v0.Symbol.Global, to: v0.Symbol.Global)
        extends TreePatch
    case class AtomicPatch(underlying: Patch) extends Patch
    case class LintPatch(message: Diagnostic) extends Patch
    case class Concat(a: Patch, b: Patch) extends Patch
    case object EmptyPatch extends Patch with LowLevelPatch
  }
}
