package scalafix.internal.util

import scala.meta._
import scala.meta.Dialect
import scalafix.v0._

object DenotationOps {
  val defaultDialect: Dialect =
    dialects.Scala212.copy(allowMethodTypes = true, allowTypeLambdas = true)

  def resultType(
      symbol: Symbol,
      denot: Denotation,
      dialect: Dialect): Option[Type] = {
    def getDeclType(tpe: Type): Type = tpe match {
      case Type.Method(_, tpe) if denot.isMethod => tpe
      case Type.Lambda(_, tpe) if denot.isMethod => getDeclType(tpe)
      case Type.Method((Term.Param(_, _, Some(tpe), _) :: Nil) :: Nil, _)
          if denot.isVar =>
        // Workaround for https://github.com/scalameta/scalameta/issues/1100
        tpe
      case x =>
        x
    }
    val signature =
      if (denot.isVal || denot.isMethod || denot.isVar) {
        denot.signature
      } else {
        throw new UnsupportedOperationException(
          s"Can't parse type for denotation $denot, denot.info=${denot.signature}")
      }
    val input = Input.VirtualFile(symbol.syntax, signature)
    (dialect, input).parse[Type].toOption.map(getDeclType)
  }
}
