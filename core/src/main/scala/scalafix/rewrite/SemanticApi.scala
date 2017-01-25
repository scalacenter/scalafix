package scalafix.rewrite

import scala.meta._

/** A custom semantic api for scalafix rewrites.
  *
  * The scalafix semantic api is a bottom-up approach to build a semantic
  * metaprogramming toolkit. We start with use cases and only implement the
  * necessary interface for those use-cases. The scala.meta semantic api is exploring
  * a top-down approach, by first defining the interface and then let use-cases
  * fit the implementation. Maybe one day, the lessons learned in the scalafix
  * can help improve the design of the scala.meta semantic api, and vice-versa.
  *
  * See [[ExplicitImplicit]] for an example usage of this semantic api.
  */
trait SemanticApi {

  /** Returns the type annotation for given val/def. */
  def typeSignature(defn: Defn): Option[Type]

  /** Returns the fully qualified name of this name, or none if unable to find it*/
  def fqn(name: Ref): Option[Ref]

  /** Returns all used refs in this compilation unit */
  def usedFqns: Seq[Ref]

  /** Returns true if importee is not used in this compilation unit, false otherwise */
  def isUnusedImport(importee: Importee): Boolean
}
