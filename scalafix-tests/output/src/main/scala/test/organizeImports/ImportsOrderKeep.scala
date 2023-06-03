package test.organizeImports

import test.organizeImports.QuotedIdent.`a.b`.`{ d }`.e
import test.organizeImports.QuotedIdent._
import test.organizeImports.QuotedIdent.`a.b`.{c => _, _}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration
import scala.concurrent._
import scala.concurrent.{Promise, Future}

object ImportsOrderKeep
