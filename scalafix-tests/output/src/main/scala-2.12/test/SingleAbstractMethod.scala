package test

import java.lang.{Runnable, Thread}

trait WithParams { def doit(a: Int, b: Int): Int }

class SingleAbstractMethod {
  val runnable1: Runnable = () => println("Run!")
  var runnable2: Runnable = () => println("Run!")
  def runnable3: Runnable = () => println("Run!")
  val doer: WithParams = (a, b) => a + b
  new Thread(() => println("Hello, Thread!"))
}