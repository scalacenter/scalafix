package test.organizeImports

object QuotedIdent {
  object `a.b` {
    object c
    object `{ d }` {
      object e
    }
  }

  object `macro`
}

object DollarIdent {
  val `a$b`: Int = 3
  val plain: Int = 4
}
