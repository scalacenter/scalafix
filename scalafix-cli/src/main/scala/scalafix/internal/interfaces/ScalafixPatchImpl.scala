package scalafix.internal.interfaces

import scalafix.Patch
import scalafix.interfaces.ScalafixPatch
import scalafix.interfaces.ScalafixTextEdit
import scalafix.internal.patch.PatchInternals
import scalafix.internal.v1.ValidatedArgs
import scalafix.v0
import scalafix.v0.RuleCtx

case class ScalafixPatchImpl(patch: Patch)(
    args: ValidatedArgs,
    ctx: RuleCtx,
    index: v0.SemanticdbIndex
) extends ScalafixPatch {

  override def textEdits(): Array[ScalafixTextEdit] =
    PatchInternals
      .treePatchApply(patch)(ctx, index)
      .map { t =>
        ScalafixTextEditImpl(
          PositionImpl.fromScala(t.tok.pos),
          t.newTok
        ): ScalafixTextEdit
      }
      .toArray

  override def isAtomic: Boolean =
    patch.isInstanceOf[Patch.internal.AtomicPatch]
}
