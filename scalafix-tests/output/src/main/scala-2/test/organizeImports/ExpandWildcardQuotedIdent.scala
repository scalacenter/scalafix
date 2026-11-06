package test.organizeImports

import test.organizeImports.DollarIdent._
import test.organizeImports.QuotedIdent.{`a.b`, `macro`}

object ExpandWildcardQuotedIdent {
  val x = `macro`
  val y = `a.b`
  val z = `a$b` + plain
}
