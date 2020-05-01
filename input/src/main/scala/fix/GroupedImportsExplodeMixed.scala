/*
rules = [OrganizeImports]
OrganizeImports {
  groupedImports = Explode
}
 */
package fix

import scala.collection.immutable._
import scala.collection.mutable.{Map, Seq => S, Buffer => _, _}

object GroupedImportsExplodeMixed {
  val m: Map[Int, Int] = ???
}
