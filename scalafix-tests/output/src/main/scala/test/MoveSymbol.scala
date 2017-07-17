package fix

import com.geirsson.{ fastmath, mutable }
import com.geirsson.mutable.{ CoolBuffer, unsafe }
import com.geirsson.mutable.unsafe.CoolMap

object MoveSymbol {
  fastmath.sqrt(9)
  val u: unsafe.CoolMap[Int, Int] = CoolMap.empty[Int, Int]
  val x: CoolBuffer[Int] = CoolBuffer.empty[Int]
  val y: mutable.CoolBuffer[Int] = mutable.CoolBuffer.empty[Int]
  val z: com.geirsson.mutable.CoolBuffer[Int] =
    com.geirsson.mutable.CoolBuffer.empty[Int]
}
