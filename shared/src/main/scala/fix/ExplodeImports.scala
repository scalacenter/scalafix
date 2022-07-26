package fix

object ExplodeImports {
  object Wildcard1 {
    object a
    object b
    object c
    object d
  }

  object Wildcard2 {
    object a
    object b
  }

  object Unimport1 {
    object a
    object b
    object c
    object d
  }

  object Unimport2 {
    object a
    object b
    object c
    object d
  }

  object Rename1 {
    object a
    object b
    object c
    object d
  }

  object Rename2 {
    object a
    object b
    object c
  }

  object Dedup {
    object a
    object b
    object c
  }

  object FormatPreserving {
    object g1 {
      object a
      object b
    }

    object g2 {
      object c
      object d
    }
  }
}
