package scalafix
package patch

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.tokens.Token
import scalafix.syntax._
import scalafix.patch.TokenPatch.Add
import scalafix.patch.TokenPatch.Remove
import scalafix.patch.TreePatch.ImportPatch
import scalafix.diff.DiffUtils
import scalafix.internal.patch.ImportPatchOps
import scalafix.internal.patch.ReplaceSymbolOps
import scalafix.internal.util.Failure
import scalafix.internal.util.TokenOps
import scalafix.internal.rule.RuleCtxImpl
import scalafix.lint.LintMessage
import scalafix.patch.TreePatch.ReplaceSymbol
import scalafix.rule.RuleName
import org.scalameta.logger

/** A data structure that can produce a .patch file.
  *
  * The best way to build a Patch is with a RuleCtx inside a Rule.
  * For example, `Rule.syntactic(ctx => ctx.addLeft(ctx.tree.tokens.head): Patch)`
  *
  * Patches can be composed with Patch.+ and Patch.++. A Seq[Patch] can be combined
  * into a single patch with `Seq[Patch](...).asPatch` with `import scalafix._`.
  *
  * Patches are split into low-level token patches and high-level tree patches.
  * A token patch works on scala.meta.Token and provides surgical precision over
  * how details like formatting are managed by the rule.
  *
  * NOTE: Patch current only works for a single file, but it may be possible
  * to add support in the future for combining patches for different files
  * with Patch + Patch.
  */
sealed abstract class Patch {
  // NOTE: potential bottle-neck, this might be very slow for large
  // patches. We might want to group related patches and enforce some ordering.
  def +(other: Patch): Patch =
    if (this eq other) this
    else if (isEmpty) other
    else if (other.isEmpty) this
    else Concat(this, other)
  def +(other: Option[Patch]): Patch =
    this.+(other.getOrElse(Patch.empty))
  def ++(other: Iterable[Patch]): Patch = other.foldLeft(this)(_ + _)
  def isEmpty: Boolean = this == EmptyPatch
  def nonEmpty: Boolean = !isEmpty
}

//////////////////////////////
// Low-level patches
//////////////////////////////
trait LowLevelPatch
abstract class TokenPatch(val tok: Token, val newTok: String)
    extends Patch
    with LowLevelPatch {
  override def toString: String =
    if (newTok.isEmpty)
      s"TokenPatch.Remove(${logger.revealWhitespace(tok.structure)})"
    else
      s"TokenPatch.${this.getClass.getSimpleName}(${logger.revealWhitespace(
        tok.syntax)}, ${tok.structure}, $newTok)"
}
private[scalafix] object TokenPatch {
  case class Remove(override val tok: Token) extends TokenPatch(tok, "")
  case class Add(
      override val tok: Token,
      addLeft: String,
      addRight: String,
      keepTok: Boolean = true)
      extends TokenPatch(
        tok,
        s"""$addLeft${if (keepTok) tok else ""}$addRight""")

}

//////////////////////////////
// High-level patches
//////////////////////////////
abstract class TreePatch extends Patch
private[scalafix] object TreePatch {
  abstract class ImportPatch extends TreePatch
  case class RemoveGlobalImport(symbol: Symbol) extends ImportPatch
  case class RemoveImportee(importee: Importee) extends ImportPatch
  case class AddGlobalImport(importer: Importer) extends ImportPatch
  case class AddGlobalSymbol(symbol: Symbol) extends ImportPatch
  case class ReplaceSymbol(from: Symbol.Global, to: Symbol.Global)
      extends TreePatch
}

// implementation detail
private[scalafix] case class LintPatch(message: LintMessage) extends Patch
private[scalafix] case class Concat(a: Patch, b: Patch) extends Patch
private[scalafix] case object EmptyPatch extends Patch with LowLevelPatch

object Patch {

  /** Combine a sequence of patches into a single patch */
  def fromIterable(seq: Iterable[Patch]): Patch =
    seq.foldLeft(empty)(_ + _)

  /** A patch that does no diff/rule */
  val empty: Patch = EmptyPatch

  private def merge(a: TokenPatch, b: TokenPatch): TokenPatch = (a, b) match {
    case (add1: Add, add2: Add) =>
      Add(
        add1.tok,
        add1.addLeft + add2.addLeft,
        add1.addRight + add2.addRight,
        add1.keepTok && add2.keepTok)
    case (_: Remove, add: Add) => add.copy(keepTok = false)
    case (add: Add, _: Remove) => add.copy(keepTok = false)
    case (rem: Remove, rem2: Remove) => rem
    case _ => throw Failure.TokenPatchMergeError(a, b)
  }

  private[scalafix] def printLintMessages(
      patches: Map[RuleName, Patch],
      ctx: RuleCtx): Unit = {

    patches.foreach {
      case (name, patch) =>
        Patch.lintMessages(patch, ctx, name).foreach { msg =>
          // Set the lint message owner. This allows us to distinguish
          // LintCategory with the same id from different rules.
          ctx.printLintMessage(msg, name)
        }
    }
  }
  private[scalafix] def lintMessages(
      patch: Patch,
      ctx: RuleCtx,
      owner: RuleName): List[LintMessage] = {
    val builder = List.newBuilder[LintMessage]
    foreach(patch) {
      case LintPatch(lint) => {

        val isEnabled =
          ctx match {
            case impl: RuleCtxImpl => {
              val ruleName = lint.category.key(owner)
              val position = lint.position
              impl.escapeHatch.isEnabled(ruleName, position)
            }
            case _ => true
          }

        if (isEnabled) {
          builder += lint
        }
      }
      case _ =>
    }
    builder.result()
  }

  // Patch.apply and Patch.lintMessages package private. Feel free to use them
  // for your application, but please ask on the Gitter channel to see if we
  // can expose a better api for your use case.
  private[scalafix] def apply(
      p: Patch,
      ctx: RuleCtx,
      index: Option[SemanticdbIndex]
  ): String = {
    treePatchApply(p)(ctx, index.getOrElse(SemanticdbIndex.empty))
  }

  private def treePatchApply(
      patch: Patch)(implicit ctx: RuleCtx, index: SemanticdbIndex): String = {
    val base = underlying(patch)
    val moveSymbol = underlying(
      ReplaceSymbolOps.naiveMoveSymbolPatch(base.collect {
        case m: ReplaceSymbol => m
      }))
    val patches = base.filterNot(_.isInstanceOf[ReplaceSymbol]) ++ moveSymbol
    val tokenPatches = patches.collect { case e: TokenPatch => e }
    val importPatches = patches.collect { case e: ImportPatch => e }
    val importTokenPatches = {
      val result = ImportPatchOps.superNaiveImportPatchToTokenPatchConverter(
        ctx,
        importPatches)
      Patch
        .underlying(result.asPatch)
        .collect {
          case x: TokenPatch => x
          case els =>
            throw Failure.InvariantFailedException(
              s"Expected TokenPatch, got $els")
        }
    }
    tokenPatchApply(ctx, importTokenPatches ++ tokenPatches)
  }

  private def tokenPatchApply(
      ctx: RuleCtx,
      patches: Iterable[TokenPatch]): String = {
    val patchMap = patches
      .groupBy(x => TokenOps.hash(x.tok))
      .mapValues(_.reduce(merge).newTok)
    ctx.tokens.toIterator
      .map(tok => patchMap.getOrElse(TokenOps.hash(tok), tok.syntax))
      .mkString
  }

  private def underlying(patch: Patch): Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    foreach(patch) {
      case _: LintPatch =>
      case els =>
        builder += els
    }
    builder.result()
  }

  private[scalafix] def isOnlyLintMessages(patch: Patch): Boolean = {
    // TODO(olafur): foreach should really return Stream[Patch] for early termination.
    var onlyLint = true
    var hasLintMessage = false
    foreach(patch) {
      case _: LintPatch => hasLintMessage = true
      case _ => onlyLint = false
    }
    hasLintMessage && onlyLint
  }

  private def foreach(patch: Patch)(f: Patch => Unit): Unit = {
    def loop(patch: Patch): Unit = patch match {
      case Concat(a, b) =>
        loop(a)
        loop(b)
      case EmptyPatch => // do nothing
      case els =>
        f(els)
    }
    loop(patch)
  }

  def unifiedDiff(original: Input, revised: Input): String = {
    DiffUtils.unifiedDiff(
      original.label,
      revised.label,
      new String(original.chars).lines.toList,
      new String(revised.chars).lines.toList,
      3)
  }
}
