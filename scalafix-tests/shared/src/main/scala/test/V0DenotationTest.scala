import scala.collection
import scala.collection.immutable
import scala.collection.mutable.{
  Map,
  Set
} // Challenge to make sure the scoping is correct

class V0DenotationTest(
    iset: immutable.Set[Int],
    cset: collection.Set[Int],
    imap: immutable.Map[Int, Int],
    cmap: collection.Map[Int, Int]) {
  iset + 1
  iset - 2
  cset + 1
  cset - 2

  cmap + (2 -> 3)
  cmap + ((4, 5))
  imap + (2 -> 3)
  imap + ((4, 5))

  // Map.zip
  imap.zip(List())
  List().zip(List())
}
