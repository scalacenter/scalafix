package tests

import java.io.Serializable
import scala.language.reflectiveCalls

object ExplicitResultTypesRefinement {
  val field: field = new field
  class field extends Serializable {
    val results: List[Int] = List(1)
  }
  val conflict: conflict2 = new conflict2
  class conflict2 extends Serializable {
    val results: List[Int] = List(1)
  }
  class conflict
  class conflict1
  def method(param: Int): method = new method(param)
  class method(param: Int) extends Serializable {
    val results: List[Int] = List(param)
  }
  def curried(param: Int)(param2: Int, param3: String): curried = new curried(param)(param2, param3)
  class curried(param: Int)(param2: Int, param3: String) extends Serializable {
    val results: List[Int] = List(param2, param3.length(), param)
  }
  def tparam[T <: CharSequence](e: T): tparam[T] = new tparam[T](e)
  class tparam[T <: CharSequence](e: T) extends Serializable {
    val results: List[Int] = List(e.length())
  }
  def app(): Unit = {
    println(field.results)
    println(method(42).results)
  }
}