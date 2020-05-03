/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Keep
  importsOrder = SymbolsFirst
}
 */
package fix

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent._
import scala.concurrent.duration
import scala.concurrent.{Promise, Future}

object ImportsOrderSymbolsFirst
