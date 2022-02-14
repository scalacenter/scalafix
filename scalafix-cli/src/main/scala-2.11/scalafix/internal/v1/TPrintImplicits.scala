package scalafix.internal.v1

import java.nio.file.PathMatcher

import scala.meta.io.AbsolutePath

import metaconfig.Conf
import pprint.TPrint
import scalafix.internal.config.ScalafixConfig

class TPrintImplicits {
  implicit val absolutePathPrint: TPrint[AbsolutePath] =
    TPrint.make[AbsolutePath](_ => "<path>")
  implicit val pathMatcherPrint: TPrint[PathMatcher] =
    TPrint.make[PathMatcher](_ => "<glob>")
  implicit val confPrint: TPrint[Conf] =
    TPrint.make[Conf](implicit cfg => TPrint.implicitly[ScalafixConfig].render)

  implicit def optionPrint[T](implicit
      ev: pprint.TPrint[T]
  ): TPrint[Option[T]] =
    TPrint.make { implicit cfg =>
      ev.render
    }
  implicit def iterablePrint[C[x] <: Iterable[x], T](implicit
      ev: pprint.TPrint[T]
  ): TPrint[C[T]] =
    TPrint.make { implicit cfg =>
      s"[${ev.render} ...]"
    }
}
