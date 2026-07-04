/*
rules = DisableSyntax
DisableSyntax.noVars = true
DisableSyntax.regex = [
  # adjacent strings concatenate, so that the pattern does not match this config comment
  "print""ln"
  {
    id = offensive
    pattern = "[Pp]imp"
    message = "Please consider a less offensive word such as Extension"
  }
]
 */
package test.escapeHatch

// https://github.com/scalacenter/scalafix/issues/507
object Issue507 {

  // the regex reports matches in code and in regular comments
  println("reported") // assert: DisableSyntax.println

  val pimpMyLibrary = 1 // assert: DisableSyntax.offensive

  /* pimp in a regular comment */ // assert: DisableSyntax.offensive
  val regularComment = 1

  // an ok anchor naming the pattern-derived id must not report itself
  println("suppressed") // scalafix:ok println

  // neither must a multi-line ok anchor
  println("suppressed") /* scalafix:ok
  println */

  // an ok anchor whose explanation matches a pattern it suppresses must not
  // report itself
  val pimped = 1 // scalafix:ok; avoid pimp

  // anchor text is never linted, even when the anchor names another rule
  var vp = 1 // scalafix:ok var; pimp

  // scalafix:off println
  println("suppressed")
  // an on anchor naming the pattern-derived id must not report itself
  // scalafix:on println

  println("reported again") // assert: DisableSyntax.println

  // anchors suppressing only their own text are unused
  /* scalafix:ok println */ // assert: UnusedScalafixSuppression
  /* scalafix:off println */ // assert: UnusedScalafixSuppression
}
