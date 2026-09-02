package test.organizeImports

object QuotedIdent {
  object `a.b` {
    object c
    object `{ d }` {
      object e
    }
  }

  object `macro`
  object `export` {
    object Other
    object SimpleSpanProcessor
  }
  object `given` {
    object Other
  }
}
