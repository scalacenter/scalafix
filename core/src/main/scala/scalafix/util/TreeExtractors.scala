package scalafix.util
import scala.meta._
object TreeExtractors {
  object `:WithParent:` {
    def unapply(tree: Tree): Option[(Tree, Tree)] =
      tree.parent.map(parent => tree -> parent)
  }

}
