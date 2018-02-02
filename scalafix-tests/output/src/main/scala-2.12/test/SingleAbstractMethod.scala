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
  val runnable1:   Runnable =  () =>    println("runnable1!") 
  var runnable2:   Runnable =  () =>    println("runnable2!") 
  def runnable3:   Runnable =  () =>    println("runnable3!") 
  val runnable4:   Runnable =
     () =>    {
      val runnable5:   Runnable = 
         () =>    println("runnable5!")
      
    }
  val runnable6:   Runnable =  () =>    println("runnable6!") 
  val runnable6:   Runnable =  () =>    println("runnable7!") 
  val withParams:   WithParams =   (a: Int, b: Int) =>    a + b 
  new Thread(   () =>    println("anon1!") )
  new Thread(   () =>    println("anon2!") )
  val timer = new TimerTask { def run(): Unit = println("timer!") }
  val foo = new Foo("t")
  val ab = new A with B { def f(a: Int): Int = a }
  val c1:   C =   (a: Int) =>    a 
  val c2: B = new C { def f(a: Int): Int = a }
  val d: D = () => 42
}
