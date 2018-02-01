package test

import java.util.TimerTask
import java.lang.Runnable

trait WithParams { def doit(a: Int, b: Int): Int }
class Foo(a: String) extends TimerTask {
  def run(): Unit = println(a)
}
trait A { def f(a: Int): Int }
trait B { def f(a: Int): Int }
trait C extends B

class SingleAbstractMethod {
  val runnable1 = new Runnable(){ def run(): Unit = println("runnable1!") }
  var runnable2 = new Runnable(){ def run(): Unit = println("runnable2!") }
  def runnable3 = new Runnable(){ def run(): Unit = println("runnable3!") }
  val runnable4 = new Runnable(){
    def run(): Unit = {
      val runnable5 = new Runnable(){ 
        def run(): Unit = println("runnable5!")
      }
    }
  }
  val withParams = new WithParams() { def doit(a: Int, b: Int): Int = a + b }
  new Thread(new Runnable(){ def run(): Unit = println("anon1!")} )
  new Thread(new Runnable(){ def run(): Unit = println("anon2!")} )
  val timer = new TimerTask { def run(): Unit = println("timer!") }
  val foo = new Foo("t")
  val ab = new A with B { def f(a: Int): Int = a }
  val c1 = new C() { def f(a: Int): Int = a }
  val c2: B = new C { def f(a: Int): Int = a }
}