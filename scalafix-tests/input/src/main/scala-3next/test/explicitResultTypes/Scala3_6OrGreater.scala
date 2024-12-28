/*
rules = ExplicitResultTypes
ExplicitResultTypes.skipSimpleDefinitions = false
*/
package test.explicitResultTypes

trait Order[T]:
  extension (values: Seq[T]) def toSorted: Seq[T] = ???
  def compare(x: T, y: T): Int

given List[Int] => Object = new:
  def foo() = 1

given Order[Int]:
  def compare(x: Int, y: Int) = ???

given listOrdering: [T: Order as elementOrder] => Order[List[T]]:
  def compare(x: List[T], y: List[T]) = elementOrder.compare(x.head, y.head)
