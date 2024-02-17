package test

class LegacySyntheticsTest {
  import scala.language.implicitConversions
  implicit def conv[F[A], A](a: A): Throwable = ???
  1: Throwable
}
