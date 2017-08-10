package scalafix.reflect

import scala.meta._
import scalafix.Mirror
import scalafix.Rewrite
import scalafix.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder

object ScalafixReflect {
  def syntactic: ConfDecoder[Rewrite] =
    fromLazyMirror(_ => None)

  def semantic(mirror: Mirror): ConfDecoder[Rewrite] =
    fromLazyMirror(_ => Some(mirror))

  def fromLazyMirror(mirror: LazyMirror): ConfDecoder[Rewrite] =
    rewriteConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(mirror),
        baseRewriteDecoders(mirror)
      )
    )
}
