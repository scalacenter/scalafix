/*
rules = DisableSyntax
DisableSyntax.noValPatterns = true
*/
package test.disableSyntax

case object DisableSyntaxNoValPatterns {
  val Right(notFound) = 1.asInstanceOf[Either[String, String]] // assert: DisableSyntax.noValPatterns
  var Right(shame) = 1.asInstanceOf[Either[String, String]]  // assert: DisableSyntax.noValPatterns
  val _ = Right("it works")
  val itWorks = Left("42")
  val (works, works2) = (1, 1)
  val ((works3, works4), works5) = ((1, 1), 1)
  val (works6, _) = (1, Left(42))
  case class TestClass(a: Int, b: Int)
  val TestClass(a, b) = TestClass(1, 1) /* assert: DisableSyntax.noValPatterns
      ^^^^^^^^^^^^^^^
  Pattern matching in val assignment can result in match error, use "_ match { ... }" with a fallback case instead.*/
  val TestClass(c, _) = TestClass(1, 1) /* assert: DisableSyntax.noValPatterns
      ^^^^^^^^^^^^^^^
  Pattern matching in val assignment can result in match error, use "_ match { ... }" with a fallback case instead.*/
}