/*
rules = DisableSyntax
DisableSyntax.noVars = true
DisableSyntax.noNulls = true
DisableSyntax.noReturns = true
DisableSyntax.noThrows = true
DisableSyntax.noTabs = true
DisableSyntax.noSemicolons = true
DisableSyntax.noXml = true
DisableSyntax.noFinalize = true
DisableSyntax.regex = [
  {
    id = offensive
    pattern = "[P|p]imp"
    message = "Please consider a less offensive word such as Extension"
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

  // actually 7.5 million years
  Await.result(Future(42), 75.days) // assert: DisableSyntax.Await\.result

  override def finalize(): Unit = println("exit") // assert: DisableSyntax.noFinalize

  val Right(notFound) = 1.asInstanceOf[Either[String, String]]
}
