package test

import java.util.TimerTask
import java.lang.Runnable

object A {
  val foo = new Runnable {
    def run(): Unit = println("Run!")
  }
  val bar = new TimerTask {
    def run(): Unit = println("Run!") 
  }
}