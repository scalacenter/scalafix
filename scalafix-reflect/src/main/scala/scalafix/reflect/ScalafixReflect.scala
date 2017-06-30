package scalafix.reflect

import scala.meta._
import scalafix.Rewrite
import scalafix.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok

object ScalafixReflect {
  def syntactic: ConfDecoder[Rewrite] =
    fromLazyMirror(_ => None)

  def semantic(mirror: Database): ConfDecoder[Rewrite] =
    fromLazyMirror(_ => Some(mirror))

  def fromLazyMirror(mirror: LazyMirror): ConfDecoder[Rewrite] =
    rewriteConfDecoder(
      MetaconfigPendingUpstream.orElse(
        ScalafixCompilerDecoder.baseCompilerDecoder(mirror),
        baseRewriteDecoders(mirror)
      )
    )
}
