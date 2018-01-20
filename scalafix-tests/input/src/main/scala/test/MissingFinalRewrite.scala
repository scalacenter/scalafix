/*
rules = MissingFinal
MissingFinal.finalCaseClass = true
MissingFinal.finalClass = true
 */
package test

object MissingFinalRewrite {
  sealed trait Foo
  case class Bar(i: Int) extends Foo
  class Buzz extends Foo
  @deprecated("oh no!", "") class Fuzz extends Foo
  @deprecated("oh no!", "") private class FuzzP extends Foo

  final case class Ok(ok: String)
  class OkClass
  trait OkTrait

  @deprecated("oh no!", "") case class BarD(i: Int)
  object FooBar {
    case class Foo(s: String)
  }
}
