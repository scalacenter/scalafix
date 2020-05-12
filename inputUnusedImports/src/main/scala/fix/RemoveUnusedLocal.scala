/*
rules = [OrganizeImports]
OrganizeImports {
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = true
}
 */
package fix

import scala.collection.mutable.{ArrayBuffer, Buffer}

object RemoveUnusedLocal {
  import java.time.Clock
  import java.lang.{Long => JLong, Double => JDouble}

  val buffer: ArrayBuffer[Int] = ArrayBuffer.empty[Int]
  val long: JLong = JLong.parseLong("0")
}
