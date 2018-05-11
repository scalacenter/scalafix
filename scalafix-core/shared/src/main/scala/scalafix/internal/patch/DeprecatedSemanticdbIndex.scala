package scalafix.internal.patch

import scala.meta._
import scala.{meta => m}
import scalafix.SemanticdbIndex
import scalafix.internal.v1.TreePos
import scalafix.v1.SemanticDoc
import DeprecatedSemanticdbIndex.DeprecationMessage

class DeprecatedSemanticdbIndex(doc: SemanticDoc) extends SemanticdbIndex {

  @deprecated(DeprecationMessage, "0.6.0")
  final override def sourcepath: Sourcepath =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def classpath: Classpath =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def database: Database =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def names: Seq[ResolvedName] =
    throw new UnsupportedOperationException
  @deprecated(DeprecationMessage, "0.6.0")
  final override def withDocuments(documents: Seq[Document]): SemanticdbIndex =
    throw new UnsupportedOperationException

  @deprecated(DeprecationMessage, "0.6.0")
  final override def symbol(position: Position): Option[Symbol] =
    doc.symbols(position).toList.headOption.map(s => m.Symbol(s.value))
  @deprecated(DeprecationMessage, "0.6.0")
  final override def symbol(tree: Tree): Option[Symbol] =
    symbol(TreePos.symbol(tree))

  @deprecated(DeprecationMessage, "0.6.0")
  final override def denotation(symbol: Symbol): Option[Denotation] =
    ???

  @deprecated(DeprecationMessage, "0.6.0")
  final override def denotation(tree: Tree): Option[Denotation] =
    symbol(tree).flatMap(denotation)
}

object DeprecatedSemanticdbIndex {
  final val DeprecationMessage = "Use SemanticDoc instead"
}
