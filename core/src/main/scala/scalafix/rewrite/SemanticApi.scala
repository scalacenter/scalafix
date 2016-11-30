package scalafix.rewrite

import scala.meta.Defn
import scala.meta.Type
import scalafix.util.Patch

// The scala.meta semantic api is  not ready yet. For time being, we
// can implement our own simplified version of the semantic api to meet
// scalafix's custom needs.
trait SemanticApi {
  def typeSignature(defn: Defn): Option[Type]
//  def addImport(i: Type): Seq[Patch]

}
