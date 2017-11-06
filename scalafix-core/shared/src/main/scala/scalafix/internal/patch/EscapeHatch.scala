package scalafix
package internal.patch

import scalafix.internal.config.FilterMatcher

import scala.meta._
import scala.meta.tokens.Token

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
  private case class Comment(position: Position, rules: String, kind: Kind)

  private sealed abstract class Kind
  private object Kind {
    case object On extends Kind
    case object Off extends Kind
  }

  private val FilterOff = "\\s?scalafix:off\\s?(.*)".r
  private val FilterOn = "\\s?scalafix:on\\s?(.*)".r

  def apply(tree: Tree): EscapeHatch = {
    val comments =
      tree.tokens.collect {
        case comment @ Token.Comment(FilterOff(rules)) => {
          Comment(comment.pos, rules, Kind.Off)
        }
        case comment @ Token.Comment(FilterOn(rules)) => {
          Comment(comment.pos, rules, Kind.On)
        }
      }.toList

    EscapeHatch(comments)
  }

  private def apply(comments: List[Comment]): EscapeHatch = {
    val enableRules = TreeMap.newBuilder[Int, FilterMatcher]
    val disableRules = TreeMap.newBuilder[Int, FilterMatcher]

    comments.foreach {
      case Comment(position, rules, kind) => {
        val ranges =
          kind match {
            case Kind.Off => disableRules
            case Kind.On => enableRules
          }

        val matcher =
          if (rules.isEmpty) {
            FilterMatcher.matchEverything
          } else {
            FilterMatcher(
              includes = rules.split("\\s+").toSeq,
              excludes = Seq()
            )
          }

        ranges += (position.start -> matcher)
      }
    }

    new EscapeHatch(
      enableRules = enableRules.result(),
      disableRules = disableRules.result()
    )
  }
}
