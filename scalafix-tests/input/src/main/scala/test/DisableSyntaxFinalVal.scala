/*
rules = DisableSyntax
DisableSyntax.noFinalVal = true
 */
package test

object DisableSyntaxFinalVal {
  final implicit val a = 1 // assert: DisableSyntax.noFinalVal
  implicit final val b = 1 // assert: DisableSyntax.noFinalVal
  final val c = 1 // assert: DisableSyntax.noFinalVal
}
