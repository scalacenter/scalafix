package scalafix.internal.v1

import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import scalafix.internal.config.ReaderUtil

sealed abstract class Ls

object Ls {
  case object Find extends Ls
  // TODO: git ls-files

  def all: List[Ls] =
    List(Find)
  implicit val encoder: ConfEncoder[Ls] =
    ConfEncoder.StringEncoder.contramap(_.toString.toLowerCase())
  implicit val decoder: ConfDecoder[Ls] =
    ReaderUtil.fromMap(all.map(x => x.toString -> x).toMap)
}
