/*
rules = "ExplicitResultTypes"
 */
package tests

import scala.collection.mutable.ArrayBuffer

object ExplicitResultTypesArrayBuffer {
  def empty[A] = ArrayBuffer.empty[A]
}