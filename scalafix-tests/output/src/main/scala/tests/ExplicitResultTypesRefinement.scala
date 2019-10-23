package tests

import java.io.Serializable
import scala.language.reflectiveCalls

object ExplicitResultTypesRefinement {
  val method: method = new method
  class method extends Serializable {
    val results: List[Int] = List(1)
  }
  def app(): Unit = println(method.results)
  val conflict: conflict2 = new conflict2
  class conflict2 extends Serializable {
    val results: List[Int] = List(1)
  }
  class conflict
  class conflict1
}