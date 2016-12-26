package scalafix.rewrite

import scala.meta.{Defn, Term, Tree, Type}
import scala.meta.parsers.Parse

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

  def desugared[A <: Tree](tree: A)(implicit parse: Parse[A]): Option[A]

  class Desugared[T <: Tree: Parse] {
    def unapply(original: T): Option[T] = desugared(original)
  }

  object DType extends Desugared[Type]
  object DTerm extends Desugared[Term]
}
