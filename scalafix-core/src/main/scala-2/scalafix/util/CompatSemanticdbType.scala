package scalafix.util

import scala.meta.internal.semanticdb.Scope
import scala.meta.internal.semanticdb.Type

object CompatSemanticdbType {
  type SemanticdbType = Type
  type SemanticdbScope = Scope
}
