package fix

import java.lang.{Long => JLong}

import scala.collection.mutable.ArrayBuffer

object RemoveUnused {
  val buffer: ArrayBuffer[Int] = ArrayBuffer.empty[Int]
  val long: JLong = JLong.parseLong("0")
}
