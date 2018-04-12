package test

import java.util.Map
import scala.collection.immutable.TreeMap

trait TypeToTreeInput {
  type A
  val a: Int
  val b: String
  val c: TreeMap[String, String]
  val d: Int
  val e: (Int, Int)
  val e2: Int => Int
  val f: Map.Entry[Int, Int]
  val g: TypeToTreeInput.this.A
  val h: TypeToTreeInput
  val i: TypeToTreeInput.this.h.A
  def j[T]: TypeToTreeInputBox[T]#A
}

trait TypeToTreeInputBox[T] {
  type A
}
