/* ONLY
rule = SingleAbstractMethod
 */
package test

import java.util.TimerTask

class Foo(a: String) extends TimerTask {
  def run(): Unit = println(a)
}

class SingleAbstractMethod {
  val bar = new TimerTask { def run(): Unit = println("Run!") }
  val boo = new Foo("t")
}