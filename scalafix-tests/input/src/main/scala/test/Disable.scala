/*
rules = Disable
Disable.symbols = [
  "scala.Any.asInstanceOf"
  "test.Disable.D.disabledFunction"
  {
    symbol = "scala.Option.get"
    message =
      """|Option.get is the root of all evils
         |
         |If you must Option.get, wrap the code block with
         |// scalafix:off Option.get
         |...
         |// scalafix:on Option.get"""
  }
  "scala.collection.mutable.ListBuffer"
  "scala.Predef.any2stringadd"
]
*/
package test

import scala.collection.mutable.ListBuffer

case object Disable {

  case class B()
  val y = B().asInstanceOf[String] // assert: Disable.asInstanceOf
  val z = 1.asInstanceOf[String] // assert: Disable.asInstanceOf
  val x = "2".asInstanceOf[Int] // assert: Disable.asInstanceOf
  val w = List(1, 2, 3).asInstanceOf[Seq[String]] // assert: Disable.asInstanceOf

  case class D() {
    def disabledFunction: Boolean = true
  }
  val zz = D().disabledFunction // assert: Disable.disabledFunction

  case class C() {
    def asInstanceOf: String = "test"
  }
  val xx = C().asInstanceOf // OK, no errors

  trait A {
    type O
  }
  object AA extends A {
    type O = String
    def asInstanceOf: O = "test"
  }
  val yy = AA.asInstanceOf // OK, no errors
  Option(1).get /* assert: Disable.get
            ^
Option.get is the root of all evils

If you must Option.get, wrap the code block with
// scalafix:off Option.get
...
// scalafix:on Option.get
*/
  val l: ListBuffer[Int] =
    scala.collection.mutable.ListBuffer.empty[Int] // scalafix:ok Disable.ListBuffer
  List(1) + "any2stringadd" /* assert: Disable.any2stringadd
  ^
any2stringadd is disabled and it got inferred as `scala.Predef.any2stringadd[List[Int]](*)`
  */
}
