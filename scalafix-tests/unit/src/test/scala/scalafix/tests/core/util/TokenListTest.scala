package scalafix.tests.core.util

import org.scalatest.FunSuite
import scala.meta._
import scalafix.util.TokenList

class TokenListTest extends FunSuite {

  test("Prev breaks if it is given the firstToken") {
    val tokens = "".tokenize.get // this contains two tokens: beginningOfFile and endOfFile
    val firstToken = tokens.head
    val tokenList = new TokenList(tokens)
    assertThrows[java.lang.ArrayIndexOutOfBoundsException](tokenList.prev(firstToken))
  }

  test("Prev2 returns the firstToken when is given the firstToken") {
    val tokens = "".tokenize.get // this contains two tokens: beginningOfFile and endOfFile
    val firstToken = tokens.last
    val tokenList = new TokenList(tokens)
    assert(tokenList.prev2(firstToken) == firstToken)
    assert(tokenList.prev2(tokens.last) == firstToken)
  }

  test("Slice gives an empty list with same token as inputs") {
    val tokens = "val x = 2".tokenize.get
    val firstToken = tokens.head 
    val tokenList = new TokenList(tokens)
    assert(tokenList.slice(firstToken,firstToken) == Seq())
  }

  test("Next breaks gives the lastToken if it is given lastToken") {
    val tokens = "".tokenize.get
    val lastToken = tokens.last
    val tokenList = new TokenList(tokens)
    assert(tokenList.next(lastToken) == lastToken)
  }
}
