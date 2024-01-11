package scala.meta.internal.scalafix

import scala.meta.internal.semanticdb.Scala.Names

object ScalafixScalametaHacks {
  def encode(name: String): String = Names.encode(name)
}
