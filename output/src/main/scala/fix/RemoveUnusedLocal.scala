package fix

import scala.collection.mutable.ArrayBuffer

object RemoveUnusedLocal {
  import java.lang.{Long => JLong}

  val buffer: ArrayBuffer[Int] = ArrayBuffer.empty[Int]
  val long: JLong = JLong.parseLong("0")
}
