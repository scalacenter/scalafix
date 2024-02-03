package scalafix.util

import scala.meta.internal.semanticdb.XtensionSemanticdbScope
import scala.meta.internal.semanticdb.XtensionSemanticdbType

object CompatSemanticdbType {
  type SemanticdbType = XtensionSemanticdbType
  type SemanticdbScope = XtensionSemanticdbScope
}
