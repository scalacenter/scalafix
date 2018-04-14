/*
rules = MissingFinal
 */
package test

object MissingFinal {
  sealed trait Sealed
  class A extends Sealed // assert: MissingFinal.class
  abstract class B extends Sealed // assert: MissingFinal.class
  abstract case class C() extends Sealed // assert: MissingFinal.class
  trait D extends Sealed // assert: MissingFinal.trait
  final class E extends Sealed // ok - class is already final
  sealed class F extends Sealed // ok - class is also sealed
  sealed abstract class G extends Sealed // ok - abstract class is also sealed
  sealed abstract case class H() extends Sealed // ok - abstract case class is also sealed
  sealed trait I extends Sealed // ok - trait is also sealed
  object J extends Sealed // ok - it's an object
  @SuppressWarnings(Array("scalafix:MissingFinal"))
  class L extends Sealed // ok - rule is suppressed

  trait NotSealed
  class M extends NotSealed // ok - parent is not sealed
  trait N extends NotSealed // ok - parent is not sealed
}
