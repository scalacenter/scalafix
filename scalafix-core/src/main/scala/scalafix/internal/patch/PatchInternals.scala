package scalafix.internal.patch

import scala.util.control.TailCalls._

import scala.meta._

import scalafix.internal.diff.DiffUtils
import scalafix.internal.util.Failure
import scalafix.internal.util.SuppressOps
import scalafix.internal.util.TokenOps
import scalafix.internal.v0.LegacySemanticdbIndex
import scalafix.lint.RuleDiagnostic
import scalafix.patch.Patch.internal._
import scalafix.patch._
import scalafix.syntax._
import scalafix.v0
import scalafix.v0.RuleCtx
import scalafix.v1

object PatchInternals {
  case class ResultWithContext(
      fixed: String,
      patches: List[v0.Patch],
      diagnostics: List[RuleDiagnostic],
      ruleCtx: v0.RuleCtx,
      semanticdbIndex: Option[v0.SemanticdbIndex]
  )
  def merge(a: TokenPatch, b: TokenPatch): TokenPatch = (a, b) match {
    case (add1: Add, add2: Add) =>
      Add(
        add1.tok,
        add1.addLeft + add2.addLeft,
        add1.addRight + add2.addRight,
        add1.keepTok && add2.keepTok
      )
    case (_: Remove, add: Add) => add.copy(keepTok = false)
    case (add: Add, _: Remove) => add.copy(keepTok = false)
    case (rem: Remove, rem2: Remove) => rem
    case _ => throw Failure.TokenPatchMergeError(a, b)
  }

  // Patch.apply and Patch.lintMessages package private. Feel free to use them
  // for your application, but please ask on the Gitter channel to see if we
  // can expose a better api for your use case.
  def apply(
      patchesByName: Map[scalafix.rule.RuleName, scalafix.Patch],
      ctx: v0.RuleCtx,
      index: Option[v0.SemanticdbIndex],
      suppress: Boolean = false
  ): ResultWithContext = {
    if (patchesByName.values.forall(_.isEmpty) && ctx.escapeHatch.isEmpty) {
      ResultWithContext(ctx.input.text, Nil, Nil, ctx, index) // no patch
    } else {
      val idx = index.getOrElse(v0.SemanticdbIndex.empty)
      val (patch, lints) = ctx.escapeHatch.filter(patchesByName)(ctx, idx)
      val finalPatch = {
        if (suppress) {
          patch.asPatch + SuppressOps.addComments(
            ctx.tokens,
            lints.map(_.position)
          )
        } else {
          patch.asPatch
        }
      }
      val patches = treePatchApply(finalPatch)(ctx, idx)
      val atomicPatches = getPatchUnits(finalPatch).toList
      ResultWithContext(
        tokenPatchApply(ctx, patches),
        atomicPatches,
        lints,
        ctx,
        index
      )
    }
  }

  def syntactic(
      patchesByName: Map[scalafix.rule.RuleName, scalafix.Patch],
      doc: v1.SyntacticDocument,
      suppress: Boolean
  ): ResultWithContext = {
    apply(patchesByName, RuleCtx(doc), None, suppress)
  }

  def semantic(
      patchesByName: Map[scalafix.rule.RuleName, scalafix.Patch],
      doc: v1.SemanticDocument,
      suppress: Boolean
  ): ResultWithContext = {
    apply(
      patchesByName,
      RuleCtx(doc.internal.doc),
      Some(new LegacySemanticdbIndex(doc)),
      suppress
    )
  }

  def treePatchApply(patch: Patch)(implicit
      ctx: v0.RuleCtx,
      index: v0.SemanticdbIndex
  ): Iterable[TokenPatch] = {
    val base = underlying(patch)
    val moveSymbol = underlying(
      ReplaceSymbolOps.naiveMoveSymbolPatch(base.collect {
        case m: ReplaceSymbol => m
      })(ctx, index)
    )
    val patches = base.filterNot(_.isInstanceOf[ReplaceSymbol]) ++ moveSymbol
    val tokenPatches = patches.collect { case e: TokenPatch => e }
    val importPatches = patches.collect { case e: ImportPatch => e }
    val importTokenPatches = {
      val result =
        ImportPatchOps.superNaiveImportPatchToTokenPatchConverter(
          ctx,
          importPatches
        )(index)

      underlying(result.asPatch)
        .collect {
          case x: TokenPatch => x
          case els =>
            throw Failure.InvariantFailedException(
              s"Expected TokenPatch, got $els"
            )
        }
    }
    importTokenPatches ++ tokenPatches
  }

  private def tokenPatchApply(
      ctx: v0.RuleCtx,
      patches: Iterable[TokenPatch]
  ): String = {
    val patchMap = patches
      .groupBy(x => TokenOps.hash(x.tok))
      .mapValues(_.reduce(merge).newTok)
    ctx.tokens.iterator
      .map(tok => patchMap.getOrElse(TokenOps.hash(tok), tok.syntax))
      .mkString
  }

  def tokenPatchApply(
      ctx: v0.RuleCtx,
      index: Option[v0.SemanticdbIndex],
      v0Patches: Iterable[v0.Patch]
  ): String = {
    val idx = index.getOrElse(v0.SemanticdbIndex.empty)
    val patches = treePatchApply(v0Patches.asPatch)(ctx, idx)
    val patchMap = patches
      .groupBy(x => TokenOps.hash(x.tok))
      .mapValues(_.reduce(merge).newTok)
    ctx.tokens.iterator
      .map(tok => patchMap.getOrElse(TokenOps.hash(tok), tok.syntax))
      .mkString
  }

  private def underlying(patch: Patch): Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    foreach(patch) {
      case _: LintPatch =>
        ()
      case els =>
        builder += els
    }
    builder.result()
  }

  private[scalafix] def getPatchUnits(patch: Patch): Seq[Patch] = {
    val builder = Seq.newBuilder[Patch]
    foreachPatchUnit(patch) {
      case _: LintPatch => ()
      case els => builder += els
    }
    builder.result()
  }

  def isOnlyLintMessages(patch: Patch): Boolean = {
    // TODO(olafur): foreach should really return Stream[Patch] for early termination.
    var onlyLint = true
    var hasLintMessage = false
    foreach(patch) {
      case _: LintPatch => hasLintMessage = true
      case _ => onlyLint = false
    }
    patch.isEmpty || hasLintMessage && onlyLint
  }

  private def foreach(patch: Patch)(f: Patch => Unit): Unit = {
    def loop(patch: Patch): TailRec[Unit] = patch match {
      case Concat(a, b) =>
        for {
          _ <- tailcall(loop(a))
          _ <- tailcall(loop(b))
        } yield ()
      case EmptyPatch =>
        done(())
      case AtomicPatch(underlying) =>
        tailcall(loop(underlying))
      case els =>
        done(f(els))
    }
    loop(patch).result
  }

  // don't decompose Atomic Patch
  private[scalafix] def foreachPatchUnit(
      patch: Patch
  )(f: Patch => Unit): Unit = {
    def loop(patch: Patch): TailRec[Unit] = patch match {
      case Concat(a, b) =>
        for {
          _ <- tailcall(loop(a))
          _ <- tailcall(loop(b))
        } yield ()
      case els =>
        done(f(els))
    }
    loop(patch).result
  }

  def unifiedDiff(original: Input, revised: Input): String = {
    unifiedDiff(original, revised, 3)
  }

  def unifiedDiff(original: Input, revised: Input, context: Int): String = {
    DiffUtils.unifiedDiff(
      original.label,
      revised.label,
      new String(original.chars).linesIterator.toList,
      new String(revised.chars).linesIterator.toList,
      context
    )
  }

}
