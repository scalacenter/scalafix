
package test.explicitResultTypes

object RefinementConfig {
  val subclass: Seq[Int] = new Seq[Int] {
    val accidentalPublic = 42
    def apply(idx: Int): Int = ???
    def iterator: Iterator[Int] = ???
    def length: Int = ???
  }
}
