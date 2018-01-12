/* ONLY
rule = SingleAbstractMethod
 */
package test

import java.util.TimerTask

object A {
  val foo = new TimerTask {
    def run(): Unit = println("Run!")
  }
}