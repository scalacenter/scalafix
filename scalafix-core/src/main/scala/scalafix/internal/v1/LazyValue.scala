package scalafix.internal.v1
import scala.util.Try

// Simple, not stack-safe container around a lazy val.
final class LazyValue[A] private (thunk: () => A) {
  private[this] lazy val _value: Try[A] =
    Try {
      thunk()
    }
  def value: A = _value.get
}

object LazyValue {
  def now[T](e: T): LazyValue[T] = new LazyValue[T](() => e)
  def later[T](e: () => T): LazyValue[T] = new LazyValue[T](e)
}
