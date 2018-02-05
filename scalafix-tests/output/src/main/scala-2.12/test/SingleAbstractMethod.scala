package test

import java.util.TimerTask
import java.lang.Runnable

trait WithParams { def doit(a: Int, b: Int): Int }
class Foo(a: String) extends TimerTask {
  def run(): Unit = println(a)
}
trait A { def f(a: Int): String }
trait B { def f(a: Int): String }
trait C extends B
trait D { def m: String }

class SingleAbstractMethod {
  val runnable1: Runnable = () => println("runnable1!") 
  var runnable2: Runnable = () => println("runnable2!") 
  def runnable3: Runnable = () => println("runnable3!") 
  val runnable4: Runnable =
    () => {
      val runnable5: Runnable = 
        () => println("runnable5!")
      
    }
  
  val runnable6: Runnable = () => println("runnable6!") 
  val runnable7: Runnable =  () => { println("runnable7!") } 
  val withParams: WithParams =  (a, b) => a + b 
  new Thread( () => println("anon1!"))
  new Thread( () => println("anon2!"))
  val timer = new TimerTask { def run(): Unit = println("timer!") }
  val foo = new Foo("t")
  val ab = new A with B { def f(a: Int): String = "ab" }
  val c1: C =  a => "c1" 
  val c2: B = new C { def f(a: Int): String = "c2" }
  val d: D =  () => "d" 
}
