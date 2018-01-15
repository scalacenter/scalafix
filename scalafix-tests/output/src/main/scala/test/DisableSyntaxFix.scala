package test

object DisableSyntaxFix {
  implicit val a = 1
  implicit val b = 1
  val c = 1

  final case class Ok(ok: String)

  final case class Bar(i: Int)
  object FooBar {
    final case class Foo(s: String)
  }
}
