package scalafix.internal.diff

private[internal] class PeekableSource[T](it: Iterator[T]) {
  private def getNext(): Option[T] = {
    if (it.hasNext) Some(it.next())
    else None
  }
  private def setPeeker(): Unit = peeker = getNext()
  private var peeker: Option[T] = None
  setPeeker()

  def hasNext: Boolean = !peeker.isEmpty || it.hasNext
  def next(): T = {
    val ret = peeker
    setPeeker()
    ret.get
  }
  def peek: Option[T] = peeker
  def drop(n: Int): Unit = {
    it.drop(n - 1)
    setPeeker()
  }
}