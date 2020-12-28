package fix

import fix.QuotedIdent._
import fix.QuotedIdent.`a.b`.{c => _, _}
import fix.QuotedIdent.`a.b`.`{ d }`.e

import scala.concurrent._
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration

object ImportsOrderSymbolsFirst
