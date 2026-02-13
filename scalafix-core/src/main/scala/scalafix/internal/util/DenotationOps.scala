package scalafix.internal.util

import scala.meta._

import scalafix.v0._

object DenotationOps {
  val defaultDialect: Dialect =
    dialects.Scala212.withAllowTypeLambdas(true)

  def resultType(
      symbol: Symbol,
      denot: Denotation,
      dialect: Dialect
  ): Option[Type] = {
    def getDeclType(tpe: Type): Type = tpe match {
      case x: Type.Lambda if denot.isMethod => getDeclType(x.tpe)
      case x => x
    }
    val signature =
      if (denot.isVal || denot.isMethod || denot.isVar) {
        denot.signature
      } else {
        throw new UnsupportedOperationException(
          s"Can't parse type for denotation $denot, denot.info=${denot.signature}"
        )
      }
    val input = Input.VirtualFile(symbol.syntax, signature)
    (dialect, input).parse[Type].toOption.map(getDeclType)
  }
}
