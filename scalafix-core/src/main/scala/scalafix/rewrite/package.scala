package scalafix

import scala.collection.immutable.Seq

package object rewrite {
  // TODO(olafur) remove me after scalafix-nsc is remoed.
  type ScalafixRewrite = Rewrite[ScalafixMirror]
  type ScalafixCtx = RewriteCtx[ScalafixMirror]
}
