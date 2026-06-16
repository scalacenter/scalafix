package test.organizeImports

import test.organizeImports.QuotedIdent.{`a.b`, `macro`}

object ExpandWildcardQuotedIdent {
  val x = `macro`
  val y = `a.b`
}
