/*
patches.removeGlobalImports = [
  "scala.collection.mutable"
]
patches.moveSymbols = [
  { from = "scala.collection.mutable.ListBuffer"
    to = "com.geirsson.mutable.CoolBuffer" }
  { from = "scala.collection.mutable.HashMap"
    to = "com.geirsson.mutable.unsafe.CoolMap" }
  { from = "scala.math.sqrt"
    to = "com.geirsson.fastmath.sqrt" }
]
 */
package fix

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable

object MoveSymbol {
  math.sqrt(9)
  val u: mutable.HashMap[Int, Int] = HashMap.empty[Int, Int]
  val x: ListBuffer[Int] = ListBuffer.empty[Int]
  val y: mutable.ListBuffer[Int] = mutable.ListBuffer.empty[Int]
  val z: scala.collection.mutable.ListBuffer[Int] =
    scala.collection.mutable.ListBuffer.empty[Int]
}
