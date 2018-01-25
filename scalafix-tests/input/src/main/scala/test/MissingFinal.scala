/*
rules = MissingFinal
 */
package test

object MissingFinal {
  sealed trait S
  class A extends S // assert: MissingFinal.class
  class B extends S // assert: MissingFinal.class
  trait C extends S // assert: MissingFinal.trait
  final class D extends S // ok
  sealed class E extends S // ok
  sealed trait F extends S // ok

  trait NotS // ok
  class G extends NotS // ok
  trait H extends NotS // ok
}
