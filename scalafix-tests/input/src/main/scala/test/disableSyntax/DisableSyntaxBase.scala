/*
rules = DisableSyntax
DisableSyntax.noVars = true
DisableSyntax.noNulls = true
DisableSyntax.noReturns = true
DisableSyntax.noWhileLoops = true
DisableSyntax.noThrows = true
DisableSyntax.noTabs = true
DisableSyntax.noSemicolons = true
DisableSyntax.noXml = true
DisableSyntax.noFinalize = true
DisableSyntax.regex = [
  {
    id = offensive
    pattern = "[Pp]imp"
    message = "Please consider a less offensive word such as Extension"
  }
  {
    id = magicNumbers
    regex = {
      pattern = "[^=]*(\d+).*$"
      captureGroup = 1
    }
    message = "Numbers should always have their named param attached, or be defined as a val."
  }
  "Await\\.result"
]
*/
package test.disableSyntax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

case object DisableSyntaxBase {


  null /* assert: DisableSyntax.null
  ^
  null should be avoided, consider using Option instead
  */

  var a = 1                 // assert: DisableSyntax.var
  def foo: Unit = return    // assert: DisableSyntax.return
  throw new Exception("ok") // assert: DisableSyntax.throw

  "semicolon"; /* assert: DisableSyntax.noSemicolons
             ^
  semicolons are disabled */

  <a>xml</a>   /* assert: DisableSyntax.noXml
  ^
  xml literals should be avoided */

	             /* assert: DisableSyntax.noTabs
^
  tabs are disabled */

  implicit class StringPimp(value: String) { // assert: DisableSyntax.offensive
    def -(other: String): String = s"$value - $other"
  }

  implicit class RichString(value: String) { // assert: DisableSyntax.classist
    def -(other: String): String = s"$value - $other"
  }

  5 // assert: DisableSyntax.magicNumbers

  val fortyTwo = 42
  val someDays = 75.days
  // actually 7.5 million years
  Await.result(Future(fortyTwo), someDays) // assert: DisableSyntax.Await\.result

  "foo" // assert: DisableSyntax.bad-indentation

  override def finalize(): Unit = println("exit") // assert: DisableSyntax.noFinalize

  val Right(notFound) = 1.asInstanceOf[Either[String, String]]

  while (true) () /* assert: DisableSyntax.while
  ^^^^^
  while loops should be avoided, consider using recursion instead */
  do () while (true) // assert: DisableSyntax.while
}
