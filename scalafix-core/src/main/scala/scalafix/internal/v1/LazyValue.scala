package scalafix.internal.v1
import scala.util.Try
import java.util.concurrent.atomic.AtomicBoolean

// Simple, not stack-safe container around a lazy val.
final class LazyValue[A] private (thunk: () => A) {
  private val isEvaluated = new AtomicBoolean(false)
  def foreach(fn: A => Unit): Unit = {
    if (isEvaluated.get()) {
      fn(value)
    }
  }
  private[this] lazy val _value: Try[A] =
    Try {
      isEvaluated.set(true)
      thunk()
    }
  def value: A = _value.get
}

object LazyValue {
  def now[T](e: T): LazyValue[T] = new LazyValue[T](() => e)
  def later[T](e: () => T): LazyValue[T] = new LazyValue[T](e)
  def fromUnsafe[T](e: () => T): LazyValue[Option[T]] =
    new LazyValue[Option[T]](() => Try(e()).toOption)
}
