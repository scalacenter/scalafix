package scalafix.internal.v1
import scala.util.Try
import java.util.concurrent.atomic.AtomicBoolean

// Simple, not stack-safe container around a lazy val.
final class LazyValue[A] private (thunk: () => A) {
  private val _isEvaluated = new AtomicBoolean(false)
  def isEvaluated: Boolean = _isEvaluated.get()
  def foreach(fn: A => Unit): Unit = {
    if (isEvaluated) {
      fn(value)
    }
  }
  private[this] lazy val _value: Try[A] =
    Try {
      _isEvaluated.set(true)
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
