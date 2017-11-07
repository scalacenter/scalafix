package scalafix
package internal.patch

import scalafix.internal.config.FilterMatcher

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.contrib.AssociatedComments

import scala.collection.immutable.TreeMap

class EscapeHatch(
    enableRules: TreeMap[Int, FilterMatcher],
    disableRules: TreeMap[Int, FilterMatcher]) {

  def isEnabled(rule: String, position: Position): Boolean =
    !isDisabled(rule, position)

  // a rule r is disabled in position p if there is a
  // comment disabling r at position p1 < p
  // and there is no comment enabling r in position p2 where p1 < p2 < p.
  def isDisabled(rule: String, position: Position): Boolean = {
    disableRules.to(position.start).exists {
      case (disablePosition, disableRule) => {
        disableRule.matches(rule) && {
          !enableRules
            .range(disablePosition, position.start)
            .values
            .exists(
              _.matches(rule)
            )
        }
      }
    }
  }
}

object EscapeHatch {
  private sealed trait Escape
  private object Escape {
    case class UntilEOF(position: Position, rules: String, toogle: Toogle)
        extends Escape
    case class Expression(position: Position, rules: String) extends Escape
  }

  private sealed abstract class Toogle
  private object Toogle {
    case object Disable extends Toogle
    case object Enable extends Toogle
  }

  private val FilterDisable = "\\s?scalafix:off\\s?(.*)".r
  private val FilterEnable = "\\s?scalafix:on\\s?(.*)".r
  private val FilterExpression = "\\s?scalafix:ok\\s?(.*)".r

  def apply(tree: Tree, associatedComments: AssociatedComments): EscapeHatch = {
    val blocks: List[Escape] =
      tree.tokens.collect {
        case comment @ Token.Comment(FilterDisable(rules)) => {
          Escape.UntilEOF(comment.pos, rules, Toogle.Disable)
        }
        case comment @ Token.Comment(FilterEnable(rules)) => {
          Escape.UntilEOF(comment.pos, rules, Toogle.Enable)
        }
      }.toList

    val expressions: List[Escape] =
      tree.collect {
        case t => {
          val trailing = associatedComments.trailing(t)
          val leading = associatedComments.leading(t)

          (trailing ++ leading).toList.flatMap {
            _ match {
              case Token.Comment(FilterExpression(rules)) => {
                List(Escape.Expression(t.pos, rules))
              }
              case _ => Nil
            }
          }
        }
      }.flatten

    EscapeHatch(blocks ++ expressions)
  }

  private def apply(comments: List[Escape]): EscapeHatch = {
    val enableRules = TreeMap.newBuilder[Int, FilterMatcher]
    val disableRules = TreeMap.newBuilder[Int, FilterMatcher]

    def matcher(rules: String): FilterMatcher = {
      if (rules.isEmpty) {
        FilterMatcher.matchEverything
      } else {
        FilterMatcher(
          includes = rules.split("\\s+").toSeq,
          excludes = Seq()
        )
      }
    }

    comments.foreach {
      case Escape.Expression(position, rules) => {
        val ruleMatcher = matcher(rules)

        disableRules += (position.start -> ruleMatcher)
        enableRules += (position.end -> ruleMatcher)
      }
      case Escape.UntilEOF(position, rules, toogle) => {
        val toogles =
          toogle match {
            case Toogle.Disable => disableRules
            case Toogle.Enable => enableRules
          }

        toogles += (position.start -> matcher(rules))
      }
    }

    new EscapeHatch(
      enableRules = enableRules.result(),
      disableRules = disableRules.result()
    )
  }
}
