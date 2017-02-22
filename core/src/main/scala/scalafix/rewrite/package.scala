package scalafix

import scala.collection.immutable.Seq
import scalafix.util.Patch

package object rewrite {
  type ScalafixRewrite = Rewrite[ScalafixMirror]
  val ScalafixRewrite: (RewriteCtx[ScalafixMirror] => Seq[Patch]) => Rewrite[
    ScalafixMirror] =
    Rewrite.apply[ScalafixMirror]
  type ScalafixCtx = RewriteCtx[ScalafixMirror]
  type SyntaxRewrite = Rewrite[Any]
  type SyntaxCtx = RewriteCtx[None.type]
}
