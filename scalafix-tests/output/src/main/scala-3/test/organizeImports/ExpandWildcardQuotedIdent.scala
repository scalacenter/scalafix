package test.organizeImports

import test.organizeImports.DollarIdent.*
import test.organizeImports.QuotedIdent.*

object ExpandWildcardQuotedIdent {
  val x = `macro`
  val y = `a.b`
  val z = `a$b` + plain
}
