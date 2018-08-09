package scalafix.internal.testkit

class Matrix[T](array: IndexedSeq[IndexedSeq[T]], _rows: Int, _columns: Int) {
  def row(r: Int): IndexedSeq[T] = array(r)
  def column(c: Int): IndexedSeq[T] = (0 to _rows).map(i => array(i)(c))
  def rows: IndexedSeq[IndexedSeq[T]] = array
  def columns: IndexedSeq[IndexedSeq[T]] = (0 to _columns).map(column)
  def cells: IndexedSeq[T] = array.flatten
}
