package scalafix.internal.v1
import scala.util.Try
import java.util.concurrent.atomic.AtomicBoolean

// Simple, not stack-safe container around a lazy val.
final class LazyValue[A] private (
    thunk: () => A,
    _isEvaluated: AtomicBoolean = new AtomicBoolean(false)
) {
  def isEvaluated: Boolean = _isEvaluated.get()
  def foreach(fn: A => Unit): Unit = {
    if (isEvaluated) {
      fn(value)
    }
  }
  def restart(): Unit = {
    _value = null
  }
  private[this] var _value: Try[A] = null
  private[this] def computeValue(): Try[A] = {
    if (_value == null) {
      _value = Try {
        _isEvaluated.set(true)
        thunk()
      }
    }
    _value
  }
  def value: A = computeValue().get
}

object LazyValue {
  def now[T](e: T): LazyValue[T] =
    new LazyValue[T](() => e, new AtomicBoolean(true))
  def later[T](e: () => T): LazyValue[T] = new LazyValue[T](e)
  def fromUnsafe[T](e: () => T): LazyValue[Option[T]] =
    new LazyValue[Option[T]](() => Try(e()).toOption)
}
