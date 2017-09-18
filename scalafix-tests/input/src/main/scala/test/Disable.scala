/*
rule = Disable
Disable.disabledSymbols = [
  "_root_.scala.Any#asInstanceOf()Ljava/lang/Object;.",
  "_root_.test.Disable.D#disabledFunction()Z."
]
*/
package test

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
}
