/*
rules = DisableSyntax
DisableSyntax.noFinalVal = true
 */
package test

object DisableSyntaxFix {
  final implicit val a = 1
  implicit final val b = 1
  final val c = 1
}
