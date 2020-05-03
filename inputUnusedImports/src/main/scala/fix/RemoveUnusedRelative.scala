/*
rules = [OrganizeImports]
OrganizeImports {
  expandRelative = true
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = true
}
 */
package fix

import scala.collection
import collection.mutable.{ArrayBuffer, Buffer}
import java.lang
import lang.{Long => JLong, Double => JDouble}

object RemoveUnusedRelative {
  val buffer: ArrayBuffer[Int] = ArrayBuffer.empty[Int]
  val long: JLong = JLong.parseLong("0")
}
