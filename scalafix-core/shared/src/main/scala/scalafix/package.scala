import scala.meta._

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

  implicit class XtensionSeqPatch(patches: Iterable[Patch]) {
    def asPatch: Patch = Patch.fromIterable(patches)
  }
  implicit class XtensionOptionPatch(patch: Option[Patch]) {
    def asPatch: Patch = patch.getOrElse(Patch.empty)
  }
}
