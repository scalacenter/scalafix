/*
rule = ExplicitResultTypes
*/
package test.explicitResultTypes

class SkipLocalImplicits {
  trait T
  def f(): T = new T {
    implicit val i = 1
  }
  def g(): Unit = {
    class C {
      implicit val i = 2
    }
  }
  def h(): Unit = {
    implicit val i = 3
  }
}
