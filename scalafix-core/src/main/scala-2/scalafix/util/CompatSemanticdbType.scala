package scalafix.util

import scala.meta.internal.semanticdb.Type
import scala.meta.internal.semanticdb.Scope

object CompatSemanticdbType {
  type SemanticdbType = Type
  type SemanticdbScope = Scope
}
