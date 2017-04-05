package cats.data
import scala.language.higherKinds

sealed abstract class Xor[+A, +B] extends Product with Serializable {
  def map[C](f: B => C) = ???
}

object Xor extends XorFunctions {
  final case class Left[+A](a: A) extends (A Xor Nothing)
  final case class Right[+B](b: B) extends (Nothing Xor B)
}

trait XorFunctions {
  def left[A, B](a: A): A Xor B = Xor.Left(a)
  def right[A, B](b: B): A Xor B = Xor.Right(b)
}

sealed abstract class XorT[F[_], A, B](value: F[A Xor B])

sealed abstract class EitherT[F[_], A, B](value: F[Either[A, B]])
