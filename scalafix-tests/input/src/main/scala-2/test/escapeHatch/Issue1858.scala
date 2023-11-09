/*
rules = [
  "ExplicitResultTypes"
]
*/
package test.escapeHatch

import io.getquill.Literal
import io.getquill.jdbczio.Quill

@SuppressWarnings(Array("scalafix:ExplicitResultTypes"))
case class Issue1858(quill: Quill.Postgres[Literal]) {
  import quill._

  case class Organizations()

  val organizations = quote(query[Organizations])

  def orgByName = quote((name: String) =>
    organizations.filter(_ => true)
  )
}