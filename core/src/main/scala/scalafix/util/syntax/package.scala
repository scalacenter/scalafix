package scalafix.util

import scala.{meta => m}
import scala.meta._

package object syntax {

  //Allow two patterns to be combined.  Contributed by @nafg
  object & { def unapply[A](a: A) = Some((a, a)) }

  implicit class MetaOps(from: m.Tree) {
    def termNames: List[Term.Name] = {
      from collect {
        case t: Term.Name => t
      }
    }

    def typeNames: List[Type.Name] = {
      from collect {
        case t: Type.Name => t
      }
    }
  }

}
