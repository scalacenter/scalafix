package scala.meta.contrib

import scala.meta.Tokens
import scala.meta.Tree
import scala.meta.XtensionClassifiable
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Comment

@deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
class AssociatedComments private (
    leadingMap: Map[Token, List[Comment]],
    trailingMap: Map[Token, List[Comment]]
) {
  def this() = this(Map.empty, Map.empty)

  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def leading(tree: Tree): Set[Comment] =
    (for {
      token <- tree.tokens.headOption
      comments <- leadingMap.get(token)
    } yield comments).getOrElse(Nil).toSet

  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def trailing(tree: Tree): Set[Comment] =
    (for {
      token <- tree.tokens.lastOption
      comments <- trailingMap.get(token)
    } yield comments).getOrElse(Nil).toSet

  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def hasComment(tree: Tree): Boolean =
    trailing(tree).nonEmpty || leading(tree).nonEmpty
}

@deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
object AssociatedComments extends AssociatedComments {
  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def apply(tree: Tree): AssociatedComments = apply(tree.tokens)

  @deprecated("AssociatedComments has been deprecated in scalameta", "v0.14.6")
  def apply(tokens: Tokens): AssociatedComments = {
    import scala.meta.tokens.Token._

    val leadingBuilder = Map.newBuilder[Token, List[Comment]]
    val trailingBuilder = Map.newBuilder[Token, List[Comment]]
    val leading = List.newBuilder[Comment]
    val trailing = List.newBuilder[Comment]
    var isLeading = true
    var lastToken: Token = tokens.head

    tokens.foreach {
      case c: Comment =>
        if (isLeading) leading += c
        else trailing += c
      case BOF() =>
        isLeading = true
      case LF() =>
        isLeading = true
      case EOF() =>
        val l = leading.result()
        val t = trailing.result()
        if (l.nonEmpty || t.nonEmpty) {
          trailingBuilder += lastToken -> (l ::: t)
        }
      case Trivia() =>
      case currentToken =>
        val t = trailing.result()
        if (t.nonEmpty) {
          trailingBuilder += lastToken -> t
          trailing.clear()
        }
        val l = leading.result()
        if (l.nonEmpty) {
          leadingBuilder += currentToken -> l
          leading.clear()
        }
        if (!currentToken.is[Comma]) {
          lastToken = currentToken
        }
        isLeading = false
    }

    new AssociatedComments(leadingBuilder.result(), trailingBuilder.result())
  }
}
