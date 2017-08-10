package object scalafix {

  type SemanticCtx = scalafix.util.SemanticCtx
  val SemanticCtx = scalafix.util.SemanticCtx

  // These are remnants from scala.meta.Mirror which got removed in 2.0.0-M2.
  // and kept here for compatibility with existing rewrites.
  @deprecated("Renamed to SemanticCtx", "0.5.0")
  type Mirror = scalafix.util.SemanticCtx
  @deprecated("Renamed to SemanticCtx", "0.5.0")
  val Mirror = scalafix.util.SemanticCtx

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
