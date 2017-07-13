package fix

import com.geirsson.mutable.CoolBuffer
import com.geirsson.mutable
import com.geirsson.mutable.unsafe.CoolMap
import com.geirsson.mutable.unsafe
import com.geirsson.fastmath

object MoveSymbol {
  fastmath.sqrt(9)
  val u: unsafe.CoolMap[Int, Int] = CoolMap.empty[Int, Int]
  val x: CoolBuffer[Int] = CoolBuffer.empty[Int]
  val y: mutable.CoolBuffer[Int] = mutable.CoolBuffer.empty[Int]
  val z: com.geirsson.mutable.CoolBuffer[Int] =
    com.geirsson.mutable.CoolBuffer.empty[Int]
}
