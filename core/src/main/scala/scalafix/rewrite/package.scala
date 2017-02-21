package scalafix

package object rewrite {
  type ScalafixRewrite = Rewrite[ScalafixMirror]
  type ScalafixCtx = RewriteCtx[ScalafixMirror]
}
