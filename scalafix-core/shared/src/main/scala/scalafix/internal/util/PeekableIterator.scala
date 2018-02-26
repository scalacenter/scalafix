package scalafix.internal.util

class PeekableIterator[T](iterator: Iterator[T]) extends Iterator[T] {
  private var exhausted: Boolean = false
  private var slot: Option[T] = None
  private def fill(): Unit = {
    if (!(exhausted || slot.isDefined)) {
      if (iterator.hasNext) {
        slot = Some(iterator.next())
      } else {
        exhausted = true
        slot = None
      }
    }
  }
  override def hasNext: Boolean = {
    if (exhausted) {
      false
    } else {
      if (slot.isDefined) true
      else iterator.hasNext
    }
  }
  def peek(): Option[T] = {
    fill()
    if (exhausted) None
    else slot
  }
  def next: T = {
    if (!hasNext) throw new NoSuchElementException()
    val out = slot.getOrElse(iterator.next)
    slot = None
    out
  }
}