package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Database
import scalafix.Failure.MissingSemanticApi

import sourcecode.Enclosing

/** An extension to scala.meta Mirror with custom methods for scalafix rewrites.
  *
  * We should try to contribute all useful methods upstream to scala.meta.
  *
  * See [[ExplicitImplicit]] for an example usage of this semantic api.
  */
@deprecated("Use scala.meta.Mirror instead.", "0.4")
trait ScalafixMirror extends Mirror {

  /** Returns the type annotation for given val/def. */
  def typeSignature(defn: Defn): Option[Type]

  /** Returns the fully qualified name of this name, or none if unable to find it*/
  def fqn(name: Ref): Option[Ref]

  /** Returns true if importee is not used in this compilation unit, false otherwise */
  def isUnusedImport(importee: Importee): Boolean

}

object ScalafixMirror {
  private def unsupported(implicit enclosing: Enclosing) =
    throw MissingSemanticApi(enclosing.value)
  def fromMirror(mirror: Mirror): ScalafixMirror = new ScalafixMirror {
    override def fqn(name: Ref): Nothing = unsupported
    override def isUnusedImport(importee: Importee): Nothing = unsupported
    override def typeSignature(defn: Defn): Option[Type] = unsupported
    override def dialect: Dialect = mirror.dialect
    override def sources: Seq[Source] = mirror.sources
    override def database: Database = mirror.database
    override def symbol(ref: Ref): Completed[Symbol] = mirror.symbol(ref)
  }

  def failingMirror(implicit d: Dialect): ScalafixMirror = new ScalafixMirror {
    override def fqn(name: Ref): Option[Ref] = unsupported
    override def isUnusedImport(importee: Importee): Boolean = unsupported
    override def typeSignature(defn: Defn): Option[Type] = unsupported
    override def sources = Nil
    override def dialect: Dialect = d
    override def database: Database = unsupported
    override def symbol(ref: Ref): Completed[Symbol] = unsupported
  }
}
