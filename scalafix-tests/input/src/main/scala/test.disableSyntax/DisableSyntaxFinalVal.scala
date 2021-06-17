/*
rules = DisableSyntax
DisableSyntax.noFinalVal = true
 */
package test.disableSyntax

object DisableSyntaxFinalVal {
  final implicit val a: Int = 1 // assert: DisableSyntax.noFinalVal
  implicit final val b: Int = 1 // assert: DisableSyntax.noFinalVal
  final val c = 1 // assert: DisableSyntax.noFinalVal
}
