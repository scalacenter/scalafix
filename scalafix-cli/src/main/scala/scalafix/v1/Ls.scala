package scalafix.v1

import metaconfig.ConfDecoder
import scalafix.internal.config.ReaderUtil

sealed abstract class Ls

object Ls {
  case object Find extends Ls
  // TODO: git ls-files

  implicit val decoder: ConfDecoder[Ls] = ReaderUtil.oneOf[Ls](Find)
}
