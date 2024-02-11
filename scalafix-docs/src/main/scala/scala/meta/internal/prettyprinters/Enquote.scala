package scala.meta.internal.prettyprinters

// compatibility hack until https://github.com/scalameta/mdoc/pull/842 is released
object enquote {
  def apply(s: String, style: QuoteStyle): String = {
    style(s)
  }
}
