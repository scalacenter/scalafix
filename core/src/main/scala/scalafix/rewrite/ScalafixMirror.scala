package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Database

/** An extension to scala.meta Mirror with custom methods for scalafix rewrites.
  *
  * We should try to contribute all useful methods upstream to scala.meta.
  *
  * See [[ExplicitImplicit]] for an example usage of this semantic api.
  */
trait ScalafixMirror extends Mirror {

  /** Returns the type annotation for given val/def. */
  def typeSignature(defn: Defn): Option[Type]

  /** Returns the fully qualified name of this name, or none if unable to find it*/
  def fqn(name: Ref): Option[Ref]

  /** Returns true if importee is not used in this compilation unit, false otherwise */
  def isUnusedImport(importee: Importee): Boolean

}
