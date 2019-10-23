/*
rules = "ExplicitResultTypes"
 */
package tests

import java.io.Serializable
import scala.language.reflectiveCalls

object ExplicitResultTypesRefinement {
  val method = new Serializable {
    val results: List[Int] = List(1)
  }
  def app(): Unit = println(method.results)
  val conflict = new Serializable {
    val results: List[Int] = List(1)
  }
  class conflict
  class conflict1
}