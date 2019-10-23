package tests

import scala.collection.mutable.ArrayBuffer

object ExplicitResultTypesArrayBuffer {
  def empty[A]: ArrayBuffer[A] = ArrayBuffer.empty[A]
}