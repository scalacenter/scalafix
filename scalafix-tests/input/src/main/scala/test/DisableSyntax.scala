/* ONLY
rules = DisableSyntax
DisableSyntax.keywords = [
  var
  null
  return
  throw
]
DisableSyntax.noTabs = true
DisableSyntax.noSemicolons = true
DisableSyntax.noXml = true
*/
package test

import scala.concurrent._
import scala.concurrent.duration._

case object DisableSyntax {


  null /* assert: DisableSyntax.keywords.null
  ^
  null is disabled
  */

  var a = 1                 // assert: DisableSyntax.keywords.var
  def foo: Unit = return    // assert: DisableSyntax.keywords.return
  throw new Exception("ok") // assert: DisableSyntax.keywords.throw

  "semicolon";              // assert: DisableSyntax.noSemicolons
  <a>xml</a>                // assert: DisableSyntax.noXml
	                          // assert: DisableSyntax.noTabs

  // implicit class StringPimp(value: String) { // assert: DisableSyntax.regex.pimp
  //   def -(other: String): String = s"$value - $other"
  // }

  // // actually 7.5 million years
  // Await.result(Future(42), 75.days) // assert: DisableSyntax.regex.Await.result
}
