package fix

import scala.concurrent._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration

import fix.QuotedIdent._
import fix.QuotedIdent.`a.b`.{c => _, _}
import fix.QuotedIdent.`a.b`.`{ d }`.e

object ImportsOrderSymbolsFirst
