package scalafix.internal.patch

import scala.meta._
import scalafix.internal.diff.DiffUtils
import scalafix.internal.util.Failure
import scalafix.internal.util.SuppressOps
import scalafix.internal.util.TokenOps
import scalafix.internal.v0.LegacyRuleCtx
import scalafix.internal.v0.LegacySemanticdbIndex
import scalafix.lint.RuleDiagnostic
import scalafix.patch.Patch.internal._
import scalafix.patch._
import scalafix.syntax._
import scalafix.v0
import scalafix.v1

object PatchInternals {
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
  ): (String, List[RuleDiagnostic]) = {
    if (patchesByName.values.forall(_.isEmpty) && ctx.escapeHatch.isEmpty) {
      (ctx.input.text, Nil)
    } else {
      val idx = index.getOrElse(v0.SemanticdbIndex.empty)
      val (patch, lints) = ctx.escapeHatch.filter(patchesByName)(ctx, idx)
      val finalPatch =
        if (suppress) {
          patch + SuppressOps.addComments(ctx.tokens, lints.map(_.position))
        } else {
          patch
        }
      val patches = treePatchApply(finalPatch)(ctx, idx)
      (tokenPatchApply(ctx, patches), lints)
    }
  }

  def syntactic(
      patchesByName: Map[scalafix.rule.RuleName, scalafix.Patch],
      doc: v1.SyntacticDocument,
      suppress: Boolean
  ): (String, List[RuleDiagnostic]) = {
    apply(patchesByName, new LegacyRuleCtx(doc), None, suppress)
  }

  def semantic(
      patchesByName: Map[scalafix.rule.RuleName, scalafix.Patch],
      doc: v1.SemanticDocument,
      suppress: Boolean
  ): (String, List[RuleDiagnostic]) = {
    apply(
      patchesByName,
      new LegacyRuleCtx(doc.internal.doc),
      Some(new LegacySemanticdbIndex(doc)),
      suppress
    )
  }

  def treePatchApply(patch: Patch)(
      implicit
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

  def foreach(patch: Patch)(f: Patch => Unit): Unit = {
    def loop(patch: Patch): Unit = patch match {
      case Concat(a, b) =>
        loop(a)
        loop(b)
      case EmptyPatch =>
        ()
      case AtomicPatch(underlying) =>
        loop(underlying)
      case els =>
        f(els)
    }
    loop(patch)
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
