package test.organizeImports

import test.organizeImports.QuotedIdent.`a.b`.`{ d }`.e
import test.organizeImports.QuotedIdent.*
import test.organizeImports.QuotedIdent.`a.b`.{c as _, *}

import scala.concurrent.ExecutionContext.Implicits.*
import scala.concurrent.duration
import scala.concurrent.*
import scala.concurrent.{Promise, Future}

object ImportsOrderKeep
