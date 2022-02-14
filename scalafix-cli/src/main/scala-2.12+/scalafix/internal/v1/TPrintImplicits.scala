package scalafix.internal.v1

import java.nio.file.PathMatcher

import scala.meta.io.AbsolutePath

import metaconfig.Conf
import metaconfig.pprint._
import scalafix.internal.config.ScalafixConfig

class TPrintImplicits {
  implicit val absolutePathPrint: TPrint[AbsolutePath] =
    new TPrint[AbsolutePath] {
      def render(implicit tpc: TPrintColors): fansi.Str = fansi.Str("<path>")
    }

  implicit val pathMatcherPrint: TPrint[PathMatcher] =
    new TPrint[PathMatcher] {
      def render(implicit tpc: TPrintColors): fansi.Str = fansi.Str("<glob>")
    }

  implicit val confPrint: TPrint[Conf] =
    new TPrint[Conf] {
      def render(implicit tpc: TPrintColors): fansi.Str =
        TPrint.implicitly[ScalafixConfig].render
    }

  implicit def optionPrint[T](implicit ev: TPrint[T]): TPrint[Option[T]] =
    new TPrint[Option[T]] {
      def render(implicit tpc: TPrintColors): fansi.Str = ev.render
    }

  implicit def iterablePrint[C[x] <: Iterable[x], T](implicit
      ev: TPrint[T]
  ): TPrint[C[T]] =
    new TPrint[C[T]] {
      def render(implicit tpc: TPrintColors): fansi.Str = s"[${ev.render} ...]"
    }
}
