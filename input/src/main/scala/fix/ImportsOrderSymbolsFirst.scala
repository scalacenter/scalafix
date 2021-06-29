/*
rules = [OrganizeImports]
OrganizeImports.removeUnused = false
OrganizeImports {
  groupedImports = Keep
  importSelectorsOrder = Keep
  importsOrder = SymbolsFirst
}
 */
package fix

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent._
import scala.concurrent.duration
import scala.concurrent.{Promise, Future}

import fix.QuotedIdent.`a.b`.`{ d }`.e
import fix.QuotedIdent.`a.b`.{c => _, _}
import fix.QuotedIdent._

object ImportsOrderSymbolsFirst
