package test.explicitResultTypes

trait Order[T]:
  extension (values: Seq[T]) def toSorted: Seq[T] = ???
  def compare(x: T, y: T): Int

given Order[Int]:
  def compare(x: Int, y: Int): Int = ???

given listOrdering: [T: Order as elementOrder] => Order[List[T]]:
  def compare(x: List[T], y: List[T]): Int = elementOrder.compare(x.head, y.head)
