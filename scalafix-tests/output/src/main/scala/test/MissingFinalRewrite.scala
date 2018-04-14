package test

object MissingFinalRewrite {
  final case class Bar(i: Int)
  final case class Ok(ok: String)
  @deprecated("oh no!", "") final case class BarD(i: Int)
  object FooBar {
    final case class Foo(s: String)
  }
  case class LeaveMeAlone(x: Int) // scalafix:ok MissingFinal
}
