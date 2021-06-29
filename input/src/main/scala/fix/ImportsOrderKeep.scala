/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports {
  groupedImports = Keep
  importSelectorsOrder = Keep
  importsOrder = Keep
}
 */
package fix

import scala.concurrent.ExecutionContext.Implicits._
import fix.QuotedIdent.`a.b`.`{ d }`.e
import scala.concurrent.duration
import fix.QuotedIdent._
import scala.concurrent._
import fix.QuotedIdent.`a.b`.{c => _, _}
import scala.concurrent.{Promise, Future}

object ImportsOrderKeep
