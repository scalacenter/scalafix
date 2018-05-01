/*
rules = Disable
Disable.symbols = [
  scala.collection.mutable
]
*/
package test.escapeHatch

import scala.collection.mutable

class Issue694 {

  // overlapping switches must not interfere with each other. The one with wider range wins
  val map: mutable.Map[String, String] /* scalafix:ok */ = // assert: UnusedScalafixSuppression
    mutable.Map.empty // scalafix:ok
}
