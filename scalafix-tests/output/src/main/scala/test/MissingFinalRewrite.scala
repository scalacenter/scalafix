package test

object MissingFinalRewrite {
  sealed trait Foo
  final case class Bar(i: Int) extends Foo
  final class Buzz extends Foo
  @deprecated("oh no!", "") final class Fuzz extends Foo
  @deprecated("oh no!", "") final private class FuzzP extends Foo

  final case class Ok(ok: String)
  class OkClass
  trait OkTrait

  @deprecated("oh no!", "") final case class BarD(i: Int)
  object FooBar {
    final case class Foo(s: String)
  }
}
