/*
rules = [OrganizeImports]
OrganizeImports {
  groups = ["re:javax?\\.", "scala.", "*"]
  removeUnused = true
}
 */
package fix

object RemoveUnusedLocal {
  import UnusedImports._
  import a.{v1, v2}
  import b.v3
  import c.{v5 => w1, v6 => w2}
  import d.{v7 => unused, _}

  val x1 = v1
  val x2 = w2
  val x3 = v8
}
