package scalafix.tests.core.util

import org.scalatest.FunSuite
import scala.meta._
import scalafix.util.TokenList

class TokenListTest extends FunSuite {

  val emptyFileTokens = "".tokenize.get // this contains two tokens: BOF and EOF
  val nonEmptyFileTokens =
    """package foo
      |
      |object Bar {
      | val baz   =   10
      |}
    """.stripMargin.tokenize.get

  test("prev returns the preceding token") {
    val tokenList = TokenList(emptyFileTokens)
    assert(tokenList.prev(emptyFileTokens.last) == emptyFileTokens.head)
  }

  test("prev returns self if there is no preceding token") {
    val tokenList = TokenList(emptyFileTokens)
    assert(tokenList.prev(emptyFileTokens.head) == emptyFileTokens.head)
  }

  test("next returns the following token") {
    val tokenList = TokenList(emptyFileTokens)
    assert(tokenList.next(emptyFileTokens.head) == emptyFileTokens.last)
  }

  test("next returns self if there is no following token") {
    val tokenList = TokenList(emptyFileTokens)
    assert(tokenList.next(emptyFileTokens.last) == emptyFileTokens.last)
  }

  test("slice returns an empty seq if there is no token in between") {
    val tokenList = TokenList(emptyFileTokens)
    assert(tokenList.slice(emptyFileTokens.head, emptyFileTokens.head) == Seq())
  }

  test("slice returns all tokens between from/to tokens") {
    val Some(kwObject) = nonEmptyFileTokens.find(_.is[Token.KwObject])
    val Some(leftBrace) = nonEmptyFileTokens.find(_.is[Token.LeftBrace])
    val tokenList = TokenList(nonEmptyFileTokens)

    val slice = tokenList.slice(kwObject, leftBrace)
    assert(slice.size == 3)
    val Seq(space1, bar, space2) = slice
    assert(space1.is[Token.Space])
    assert(bar.syntax.equals("Bar"))
    assert(space2.is[Token.Space])
  }

  test("leadingSpaces returns all spaces preceding a token") {
    val Some(equals) = nonEmptyFileTokens.find(_.is[Token.Equals])
    val tokenList = TokenList(nonEmptyFileTokens)

    val spaces = tokenList.leadingSpaces(equals)
    assert(spaces.size == 3)
    val Seq(s1, s2, s3) = spaces
    assert(s1 == tokenList.prev(equals))
    assert(s2 == tokenList.prev(s1))
    assert(s3 == tokenList.prev(s2))
  }

  test(
    "leadingSpaces returns an empty seq if there are no spaces preceding a token") {
    val Some(kwPackage) = nonEmptyFileTokens.find(_.is[Token.KwPackage])
    val tokenList = TokenList(nonEmptyFileTokens)

    assert(tokenList.leadingSpaces(kwPackage) == Seq())
  }

  test("trailingSpaces returns all spaces following a token") {
    val Some(equals) = nonEmptyFileTokens.find(_.is[Token.Equals])
    val tokenList = TokenList(nonEmptyFileTokens)

    val spaces = tokenList.trailingSpaces(equals)
    assert(spaces.size == 3)
    val Seq(s1, s2, s3) = spaces
    assert(s1 == tokenList.next(equals))
    assert(s2 == tokenList.next(s1))
    assert(s3 == tokenList.next(s2))
  }

  test(
    "trailingSpaces returns an empty seq if there are no spaces following a token") {
    val Some(rightBrace) = nonEmptyFileTokens.find(_.is[Token.RightBrace])
    val tokenList = TokenList(nonEmptyFileTokens)

    assert(tokenList.trailingSpaces(rightBrace) == Seq())
  }
}
