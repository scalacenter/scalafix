package test.organizeImports

import test.organizeImports.QuotedIdent.*
import test.organizeImports.QuotedIdent.`a.b`
import test.organizeImports.QuotedIdent.`a.b` as ab
import test.organizeImports.QuotedIdent.`a.b`.{c as _, *}
import test.organizeImports.QuotedIdent.`a.b`.`{ d }`.e
import test.organizeImports.QuotedIdent.`a.b`.`{ d }`.e as E

import scala.concurrent.*
import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.*
import scala.concurrent.duration

object ImportsOrderSymbolsFirst
