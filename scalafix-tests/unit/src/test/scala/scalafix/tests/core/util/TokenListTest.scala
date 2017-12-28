package scalafix.tests.core.util

import org.scalatest.FunSuite

import scala.meta._
import scala.meta.dialects.Scala211
import scalafix.util.TokenList

class TokenListTest extends FunSuite {

  val tokens =
    """package foo
      |
      |object Bar {
      | val baz   =   10
      |}""".stripMargin.tokenize.get
  val tokenList = TokenList(tokens)

  test("prev returns the preceding token") {
    assert(tokenList.prev(tokens(1)) == tokens.head)
  }

  test("prev returns self if there is no preceding token") {
    assert(tokenList.prev(tokens.head) == tokens.head)
  }

  test("next returns the following token") {
    assert(tokenList.next(tokens.head) == tokens(1))
  }

  test("next returns self if there is no following token") {
    assert(tokenList.next(tokens.last) == tokens.last)
  }

  test("slice returns `from` if there is no more tokens in between") {
    assert(tokenList.slice(tokens.head, tokens(1)) == Seq(tokens.head))
  }

  test("slice returns empty seq if `from` and `to` tokens are the same object") {
    assert(tokenList.slice(tokens.head, tokens.head).isEmpty)
  }

  test("slice returns empty seq if `from` comes after `to`") {
    assert(tokenList.slice(tokens.last, tokens.head).isEmpty)
  }

  test("slice fails if `from` token does not exist") {
    assertThrows[NoSuchElementException] {
      tokenList.slice(tokens.head, new Token.EOF(Input.None, Scala211))
    }
  }

  test("slice fails if `to` token does not exist") {
    assertThrows[NoSuchElementException] {
      tokenList.slice(new Token.EOF(Input.None, Scala211), tokens.last)
    }
  }

  test("slice returns tokens between `from` (inclusive) and `to`") {
    val Some(kwObject) = tokens.find(_.is[Token.KwObject])
    val Some(leftBrace) = tokens.find(_.is[Token.LeftBrace])

    val slice = tokenList.slice(kwObject, leftBrace)
    assert(slice.size == 4)
    val Seq(kwObj, space1, bar, space2) = slice
    assert(kwObj == kwObject)
    assert(space1.is[Token.Space])
    assert(bar.syntax.equals("Bar"))
    assert(space2.is[Token.Space])
  }

  test("leadingSpaces returns all spaces preceding a token") {
    val Some(equals) = tokens.find(_.is[Token.Equals])

    val spaces = tokenList.leadingSpaces(equals)
    assert(spaces.size == 3)
    val Seq(s1, s2, s3) = spaces
    assert(s1 == tokenList.prev(equals))
    assert(s2 == tokenList.prev(s1))
    assert(s3 == tokenList.prev(s2))
  }

  test(
    "leadingSpaces returns an empty seq if there's no space preceding a token") {
    val Some(kwPackage) = tokens.find(_.is[Token.KwPackage])

    assert(tokenList.leadingSpaces(kwPackage) == Seq())
  }

  test("trailingSpaces returns all spaces following a token") {
    val Some(equals) = tokens.find(_.is[Token.Equals])

    val spaces = tokenList.trailingSpaces(equals)
    assert(spaces.size == 3)
    val Seq(s1, s2, s3) = spaces
    assert(s1 == tokenList.next(equals))
    assert(s2 == tokenList.next(s1))
    assert(s3 == tokenList.next(s2))
  }

  test(
    "trailingSpaces returns an empty seq if there's no space following a token") {
    val Some(rightBrace) = tokens.find(_.is[Token.RightBrace])

    assert(tokenList.trailingSpaces(rightBrace) == Seq())
  }
}
