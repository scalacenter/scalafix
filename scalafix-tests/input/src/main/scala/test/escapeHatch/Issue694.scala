/*
rules = Disable
Disable.symbols = [
  scala.collection.mutable
]
*/
package test.escapeHatch

import scala.collection.mutable

// Fails with Scala3 dialect as the second silencer is not attached to the Defn.Val
// https://github.com/scalameta/scalameta/issues/3689
class Issue694 {

  // overlapping switches must not interfere with each other. The one with wider range wins
  val map: mutable.Map[String, String] /* scalafix:ok */ = // assert: UnusedScalafixSuppression
    mutable.Map.empty // scalafix:ok
}
