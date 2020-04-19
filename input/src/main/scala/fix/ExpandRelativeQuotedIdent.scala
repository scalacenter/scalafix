/*
rules = OrganizeImports
OrganizeImports.expandRelative = true
 */

package fix

// TODO Re-enable this test case after scalacenter/scalafix#1097 is fixed.
// import ExpandRelativeQuotedIdent.`a.b`
// import `a.b`.c

object ExpandRelativeQuotedIdent {
  object `a.b` {
    object c
  }
}
