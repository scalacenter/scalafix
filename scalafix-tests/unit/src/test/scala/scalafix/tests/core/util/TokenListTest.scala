package scalafix.tests.core.util

import org.scalatest.FunSuite
import scala.meta._
import scalafix.util.TokenList

class TokenListTest extends FunSuite {

  val tokens = "".tokenize.get // this contains two tokens: beginningOfFile and endOfFile
  val firstToken = tokens.head
  val lastToken = tokens.last
  val tokenList = new TokenList(tokens)

  test("Prev returns the firstToken when is given the firstToken") {
    assert(tokenList.prev(firstToken) == firstToken)
    assert(tokenList.prev(tokens.last) == firstToken)
  }

  test("Slice gives an empty list with same token as inputs") {
    assert(tokenList.slice(firstToken, firstToken) == Seq())
  }

  test("Next breaks gives the lastToken if it is given lastToken") {
    assert(tokenList.next(lastToken) == lastToken)
  }
}
