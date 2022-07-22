package scalafix.util

import scala.meta.internal.semanticdb.XtensionSemanticdbType
import scala.meta.internal.semanticdb.XtensionSemanticdbScope

object CompatSemanticdbType {
  type SemanticdbType = XtensionSemanticdbType
  type SemanticdbScope = XtensionSemanticdbScope
}
