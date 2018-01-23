package test

import java.util.TimerTask
import java.lang.Runnable

trait WithParams { def doit(a: Int, b: Int): Int }

class SingleAbstractMethod {
  val runnable1: Runnable = () => println("Run!")
  var runnable2: Runnable = () => println("Run!")
  def runnable3: Runnable = () => println("Run!")
  val doer: WithParams = (a, b) => a + b
  new Thread(() => println("Hello, Thread!"))

  val bar = new TimerTask { def run(): Unit = println("Run!") }
}
