package fix

// TODO This test case fails.
// import ExpandRelativeQuotedIdent.`a.b`
// import `a.b`.c

object ExpandRelativeQuotedIdent {
  object `a.b` {
    object c
  }
}
