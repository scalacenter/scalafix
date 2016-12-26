package cats

import scala.language.implicitConversions

package object implicits {
  implicit class EitherOps[A, B](from: Either[A, B]) {
    def map[C](f: B => C): Either[A, C] = ???
  }
}
