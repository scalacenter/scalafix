package fix

import java.lang
import java.lang.{Long => JLong}

import scala.collection
import scala.collection.mutable.ArrayBuffer

object RemoveUnusedRelative {
  val buffer: ArrayBuffer[Int] = ArrayBuffer.empty[Int]
  val long: JLong = JLong.parseLong("0")
}
