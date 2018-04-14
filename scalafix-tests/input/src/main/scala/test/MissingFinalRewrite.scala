/*
rules = MissingFinal
 */
package test

object MissingFinalRewrite {
  case class Bar(i: Int)
  final case class Ok(ok: String)
  @deprecated("oh no!", "") case class BarD(i: Int)
  object FooBar {
    case class Foo(s: String)
  }
  case class LeaveMeAlone(x: Int) // scalafix:ok MissingFinal
}
