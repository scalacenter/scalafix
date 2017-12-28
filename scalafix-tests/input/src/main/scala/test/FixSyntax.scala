/*
rules = FixSyntax
FixSyntax.removeFinalVal = true
FixSyntax.addFinalCaseClass = true
 */
package test

object FixSyntax {
  final implicit val a = 1
  implicit final val b = 1
  final val c = 1

  final case class Ok(ok: String)

  case class Bar(i: Int)
  object FooBar {
    case class Foo(s: String)
  }
}
