import scala.meta._
import scalafix.patch.SemanticPatchOps
import scalafix.patch.SyntacticPatchOps

package object scalafix {

  type ScalafixConfig = config.ScalafixConfig
  val ScalafixConfig = config.ScalafixConfig

  type RewriteCtx = rewrite.RewriteCtx
  val RewriteCtx = rewrite.RewriteCtx

  type SemanticRewrite = rewrite.SemanticRewrite
  type Rewrite = rewrite.Rewrite
  val Rewrite = rewrite.Rewrite

  type Patch = patch.Patch
  val Patch = patch.Patch

  implicit class XtensionMirrorRewriteCtx(val ctx: RewriteCtx)(
      implicit val mirror: Mirror)
      extends SemanticPatchOps(ctx, mirror) {
    def semanticOps: SemanticPatchOps = this
  }
  implicit class XtensionRewriteCtx(val ctx: RewriteCtx)
      extends SyntacticPatchOps(ctx) {
    def semanticOps: SyntacticPatchOps = this
  }
  implicit class XtensionSeqPatch(patches: Seq[Patch]) {
    def asPatch: Patch = Patch.fromSeq(patches)
  }
  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }
}
