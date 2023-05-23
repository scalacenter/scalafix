package test.organizeImports

import test.organizeImports.QuotedIdent._
import test.organizeImports.QuotedIdent.`a.b`.{c => _, _}
import test.organizeImports.QuotedIdent.`a.b`.`{ d }`.e

import scala.concurrent._
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration

object ImportsOrderSymbolsFirst
