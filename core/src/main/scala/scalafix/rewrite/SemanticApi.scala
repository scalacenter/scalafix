package scalafix.rewrite

import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Database
import scala.collection.immutable.Seq
import scala.util.Try

import org.scalameta.logger

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
trait SemanticApi extends Mirror {

  /** Returns the type annotation for given val/def. */
  def typeSignature(defn: Defn): Option[Type]

  /** Returns the fully qualified name of this name, or none if unable to find it*/
  def fqn(name: Ref): Option[Ref]

  /** Returns true if importee is not used in this compilation unit, false otherwise */
  def isUnusedImport(importee: Importee): Boolean

  // TODO(olafur) more elegant way to combine two interfaces
  def mirror: Mirror
  override def dialect: Dialect = mirror.dialect
  override def sources: Seq[Source] = mirror.sources
  override def database: Database = mirror.database
  override def symbol(ref: Ref): Completed[Symbol] = mirror.symbol(ref)
}
