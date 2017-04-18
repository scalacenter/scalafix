import scala.meta._

package object scalafix {
  type SemanticRewriteCtx = rewrite.RewriteCtx[Mirror]
  type SyntacticRewriteCtx = rewrite.RewriteCtx[Any]

  type Rewrite[T] = rewrite.Rewrite[T]
  val Rewrite = rewrite.Rewrite

  type Patch = util.Patch
  val Patch = util.Patch

  type RewriteCtx[T] = rewrite.RewriteCtx[T]
  val RewriteCtx = rewrite.RewriteCtx

  implicit class XtensionSemanticRewriteCtx(val ctx: SemanticRewriteCtx)
      extends rewrite.SemanticPatchOps
  implicit class XtensionRewriteCtx[T](val ctx: RewriteCtx[T])
      extends rewrite.SyntacticPatchOps[T]
  implicit class XtensionSeqPatch(patches: Seq[Patch]) {
    def asPatch: Patch = Patch.fromSeq(patches)
  }
}
