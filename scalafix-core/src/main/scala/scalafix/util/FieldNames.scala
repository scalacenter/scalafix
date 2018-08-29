package scalafix.util

/** Polyfill for the upcoming `Product.productElementName` in 2.13
  *
  * See https://github.com/scala/scala/blob/4dc5b915f417bb4107f0700e19a957cb7d7d8e3b/src/library/scala/Product.scala#L52-L70
  */
trait FieldNames { self: Product =>
  def fieldName(n: Int): String =
    if (n < 0 || n >= self.productArity) {
      throw new IndexOutOfBoundsException(n.toString)
    } else {
      ""
    }
  def fieldNames: Iterator[String] = new collection.AbstractIterator[String] {
    private[this] var c: Int = 0
    private[this] val cmax = self.productArity
    def hasNext: Boolean = c < cmax
    def next(): String = { val result = fieldName(c); c += 1; result }
  }
}
