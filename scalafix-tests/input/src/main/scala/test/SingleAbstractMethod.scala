/*
rule = SingleAbstractMethod
 */
package test

import java.lang.{Runnable, Thread}

trait WithParams { def doit(a: Int, b: Int): Int }

class SingleAbstractMethod {
  val runnable1 = new Runnable(){ def run(): Unit = println("Run!")}
  var runnable2 = new Runnable(){ def run(): Unit = println("Run!")}
  def runnable3 = new Runnable(){ def run(): Unit = println("Run!")}
  val doer = new WithParams() { def doit(a: Int, b: Int): Int = a + b }
  new Thread(new Runnable(){ def run(): Unit = println("Hello, Thread!")})
}