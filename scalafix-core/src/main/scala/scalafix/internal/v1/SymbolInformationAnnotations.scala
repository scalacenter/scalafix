package scalafix.internal.v1

import scala.annotation.StaticAnnotation

object SymbolInformationAnnotations {
  class property extends StaticAnnotation
  class kind extends StaticAnnotation
  class language extends StaticAnnotation
  class access extends StaticAnnotation
  class utility extends StaticAnnotation
}
