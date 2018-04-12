package test

import java.util.Map
import scala.collection.immutable.TreeMap

trait TypeToTreeInput[A, B >: String <: CharSequence] {
  type A
  val a: Int
  val b: String
  val c: TreeMap[String, String]
  val d: Int
  val e: (Int, Int)
  val e2: Int => Int
  val f: Map.Entry[Int, Int]
  val g: TypeToTreeInput.this.A
  val h: TypeToTreeInput[Long, String]
  val i: TypeToTreeInput.this.h.A
  def j[T]: TypeToTreeInputBox[T]#A
  val k: Either[List[Long], Long]
  val l: TypeToTreeInputBox.Nested
}

trait TypeToTreeInputBox[T] { type A }
object TypeToTreeInputBox {
  class Nested
}

class TypeToTreeClass private (x: Int)(y: String)
