package scalafix.rewrite

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Database

import sourcecode.Enclosing

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

object ScalafixMirror {
  private def error(implicit enclosing: Enclosing) =
    new UnsupportedOperationException(
      s"${enclosing.value} requires the semantic api,")
  private def unsupported(implicit enclosing: Enclosing) = throw error

  def fromMirror(mirror: Mirror): ScalafixMirror = new ScalafixMirror {
    override def fqn(name: Ref): Nothing = unsupported
    override def isUnusedImport(importee: Importee): Nothing = unsupported
    override def typeSignature(defn: Defn): Option[Type] = unsupported
    override def dialect: Dialect = mirror.dialect
    override def sources: Seq[Source] = mirror.sources
    override def database: Database = mirror.database
    override def symbol(ref: Ref): Completed[Symbol] = mirror.symbol(ref)
  }

  def empty(implicit dialect: Dialect): ScalafixMirror = new ScalafixMirror {
    override def fqn(name: Ref): Option[Ref] = None
    override def isUnusedImport(importee: Importee): Boolean = false
    override def typeSignature(defn: Defn): Option[Type] = None
    override def sources = Nil
    override def dialect: Dialect = dialect
    override def database: Database = Database()
    override def symbol(ref: Ref): Completed[Symbol] = Completed.Error(error)
  }
}
