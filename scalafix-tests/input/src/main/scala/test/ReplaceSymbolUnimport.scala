/*
rules = [
  "replace:scala.concurrent.Future/com.geirsson.Future"
  "replace:scala.util.Random/com.geirsson.Random"
]
 */
package test

// The replaced symbols are brought into scope by *nested* wildcard imports
// (inside a def body and inside an object) that also bring other, used names
// into scope. The replacement's named import is added at the top level, so
// leaving the nested wildcards untouched would expose the old bindings in an
// inner scope and produce an ambiguous import (scalacenter/scalafix#376). Each
// nested wildcard must unimport the replaced name while keeping the rest.
object ReplaceSymbolUnimport {
  def run: Any = {
    import scala.concurrent._
    Future.successful(1)
    Future.successful(2)
    Promise[Int]()
  }

  object Nested {
    import scala.util._
    Random.nextInt()
    Try(1)
  }

  // The replaced name is *both* explicitly imported and wildcard-imported. The
  // explicit importee must be folded into the unimport rather than left
  // alongside it (which would re-import the old binding).
  def grouped: Any = {
    import scala.concurrent.{Future, _}
    Future.successful(3)
    Promise[Int]()
  }

  // The replaced name is renamed alongside a wildcard. The rename selector is
  // dropped (the alias usage is rewritten to the replacement) and turned into
  // an unimport so the wildcard no longer re-introduces the old binding.
  def renamed: Any = {
    import scala.concurrent.{Future => ScalaFuture, _}
    ScalaFuture.successful(4)
    Promise[Int]()
  }
}
