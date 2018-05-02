/* 
rules = MatchingUnsealedTrait
*/

trait Parent
trait A

sealed trait S extends Parent

object O {
  val a: A = null
  a match { // assert: MatchingUnsealedTrait.mustProvideCatchAll
    case dsa if false => dsa
  }

  a match {
    case dsa => dsa //fine
  }

  val s: S = null
  s match {
    case asd if false => asd //compiler already complains
  }

  def foo: A = null
  foo match { // assert: MatchingUnsealedTrait.mustProvideCatchAll
    case asd if false => asd
  }

  def foos(i: Int): Seq[S] = null
  foos(23) match {
    case s :: Nil => s
    case _ => ???
  }

  foos(23).head match {
    case _ => ???
  }
}
