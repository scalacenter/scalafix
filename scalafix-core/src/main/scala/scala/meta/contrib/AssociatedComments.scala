package scala.meta.contrib

import scala.meta.Tree
import scala.meta.tokens.Token.Comment

@deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
class AssociatedComments {
  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def leading(tree: Tree): Set[Comment] = ???
  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def trailing(tree: Tree): Set[Comment] = ???
  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def hasComment(tree: Tree): Boolean = ???
}

@deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
object AssociatedComments extends AssociatedComments
