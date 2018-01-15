/*
rules = DisableSyntax
DisableSyntax.noFinalVal = true
DisableSyntax.noNonFinalCaseClass = true
 */
package test

object DisableSyntaxFix {
  final implicit val a = 1
  implicit final val b = 1
  final val c = 1

  final case class Ok(ok: String)

  case class Bar(i: Int)
  object FooBar {
    case class Foo(s: String)
  }
}
