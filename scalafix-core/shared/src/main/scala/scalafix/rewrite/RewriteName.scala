package scalafix.rewrite

final class RewriteName(val head: String, val tail: List[String]) {
  require(
    head != RewriteName.emptyHead || tail.isEmpty,
    "Empty rewrite cannot have underlying names!"
  )
  def name: String =
    (head :: tail).mkString("+")
  def isEmpty: Boolean = head == RewriteName.emptyHead
  def +(other: RewriteName): RewriteName =
    if (isEmpty) other
    else if (other.isEmpty) this
    else new RewriteName(head, head :: tail ::: other.tail)
  override def toString: String = name
}

object RewriteName {
  private[scalafix] val emptyHead = "empty"
  final val empty = new RewriteName(emptyHead, Nil)
  def apply(name: String) = new RewriteName(name, Nil)
  implicit def generate(implicit name: sourcecode.Name): RewriteName =
    new RewriteName(name.value, Nil)
}
