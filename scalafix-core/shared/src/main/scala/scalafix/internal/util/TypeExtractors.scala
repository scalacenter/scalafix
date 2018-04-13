package scalafix.internal.util

import scala.meta.internal.{semanticdb3 => s}
import scala.meta.internal.semanticdb3.Type.{Tag => t}

object TypeExtractors {
  abstract class TypeRefExtractor(sym: String) {
    def matches(tpe: s.Type): Boolean = tpe.tag match {
      case t.TYPE_REF =>
        tpe.typeRef.exists(_.symbol == sym)
      case t.WITH_TYPE =>
        tpe.withType.exists { x =>
          x.types.lengthCompare(1) == 0 &&
          unapply(x.types.head)
        }
      case _ =>
        false
    }
    def unapply(tpe: s.Type): Boolean = matches(tpe)
  }
  object AnyRef extends TypeRefExtractor("scala.AnyRef#")
  object Nothing extends TypeRefExtractor("scala.Nothing#")
  object Any extends TypeRefExtractor("scala.Any#")
  object Product extends TypeRefExtractor("scala.Product#")
  object Serializable extends TypeRefExtractor("scala.Serializable#")
  object Wildcard extends TypeRefExtractor("local_wildcard")
  def isFunctionN(symbol: String): Boolean = {
    symbol.startsWith("scala.Function") &&
    symbol.endsWith("#")
  }

  def isTupleN(symbol: String): Boolean = {
    symbol.startsWith("scala.Tuple") &&
    symbol.endsWith("#")
  }

  object FunctionN {
    def unapply(symbol: String): Boolean = isFunctionN(symbol)
  }
  object TupleN {
    def unapply(symbol: String): Boolean = isTupleN(symbol)
  }
}
