package scalafix

import scala.collection.immutable.Seq

package object rewrite {
  // TODO(olafur) remove me after scalafix-nsc is remoed.
  type ScalafixRewrite = rewrite.Rewrite[ScalafixMirror]
  type ScalafixCtx = rewrite.RewriteCtx[ScalafixMirror]
}
