package fix

object RemoveUnusedLocal {
  import UnusedImports._
  import a.v1
  import c.{v6 => w2}
  import d.{v7 => _, _}

  val x1 = v1
  val x2 = w2
  val x3 = v8
}
