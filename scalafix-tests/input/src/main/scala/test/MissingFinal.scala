/*
rules = MissingFinal
 */
package test

object MissingFinal {
  sealed trait Sealed
  trait NotSealed

  class A extends Sealed // assert: MissingFinal.class
  class B extends Sealed // assert: MissingFinal.class
  trait C extends Sealed // assert: MissingFinal.trait
  final class D extends Sealed // ok - class is already final
  sealed class E extends Sealed // ok - class is also sealed
  sealed trait F extends Sealed // ok - trait is also sealed
  @SuppressWarnings(Array("scalafix:MissingFinal"))
  class G extends Sealed // ok - rule is suppressed

  class H extends NotSealed // ok - not leaking sealed
  trait I extends NotSealed // ok - not leaking sealed
}
