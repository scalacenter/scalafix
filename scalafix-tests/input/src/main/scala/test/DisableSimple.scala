/*
rules = Disable
Disable.symbols = [
  "scala.Any.asInstanceOf"
  "test.DisableSimple.D.disabledFunction"
  {
    symbol = "scala.Option.get"
    id = "Option.get"
    message =
      """|Option.get is the root of all evils
         |
         |If you must Option.get, wrap the code block with
         |// scalafix:off Option.get
         |...
         |// scalafix:on Option.get"""
  }
  "scala.collection.mutable"
  "scala.collection.immutable.List.drop"
  "scala.collection.LinearSeqOptimized.length"
]
Disable.ifSynthetic = [
  "scala.Predef.any2stringadd"
]
*/
package test

case object DisableSimple {
  import scala.collection.mutable.ListBuffer // ok
  import scala.collection.mutable.BitSet // ok

  case class B()
  val y = B().asInstanceOf[String] // assert: Disable.asInstanceOf
  val z = 1.asInstanceOf[String] // assert: Disable.asInstanceOf
  val x = "2".asInstanceOf[Int] // assert: Disable.asInstanceOf
  val w = List(1, 2, 3).asInstanceOf[Seq[String]] // assert: Disable.asInstanceOf

  case class D() {
    def disabledFunction: Boolean = true // assert: Disable.disabledFunction
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
  Option(1).get /* assert: Disable.Option.get
            ^
Option.get is the root of all evils

If you must Option.get, wrap the code block with
// scalafix:off Option.get
...
// scalafix:on Option.get
*/
  val l: ListBuffer[Int] = scala.collection.mutable.ListBuffer.empty[Int] // assert: Disable.mutable
  List(1) + "any2stringadd" /* assert: Disable.any2stringadd
  ^
any2stringadd is disabled and it got inferred as `any2stringadd[List[Int]](*)`
  */

  @SuppressWarnings(Array("Disable.drop", "Disable.length"))
  val _ = List(1, 2 ,3).drop(2).filter(_ > 3).reverse.length
}
