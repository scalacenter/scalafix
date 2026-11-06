package test.organizeImports

object QuotedIdent {
  object `a.b` {
    object c
    object `{ d }` {
      object e
    }
  }

  object `macro`
  object ea
  object `export` {
    object Other
    object SimpleSpanProcessor
  }
  object `given` {
    object Other
  }
}

object DollarIdent {
  val `a$b`: Int = 3
  val plain: Int = 4
}

object StarIdent {
  object *
  val plain: Int = 5
}
