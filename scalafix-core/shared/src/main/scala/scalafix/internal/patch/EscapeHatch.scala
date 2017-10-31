package scalafix
package internal.patch

import scala.meta._
import scala.meta.tokens.Token

object RangePosition {
  def apply(position: Position): RangePosition =
    RangePosition(position.start, position.end)
}

case class RangePosition(start: Int, end: Int) {
  def withIn(other: RangePosition): Boolean = {
    start <= other.start && other.end <= end
  }
}
case class PointPosition(offset: Int)

case class CommentFilter(
    start: Position,
    end: Option[Position],
    rules: Option[String]
) {
  def isEnabled(ruleName: String, rulePosition: Position): Boolean = {
    val ruleMatches =
      if (rules.nonEmpty) rules.contains(ruleName)
      else true

    ruleMatches && intersect(rulePosition)
  }

  private def intersect(rulePosition: Position): Boolean = {
    end match {
      case Some(endPos) => {
        start.start <= rulePosition.start &&
        rulePosition.end <= endPos.end
      }
      case None => {
        start.start <= rulePosition.start
      }
    }
  }
}

sealed trait AnchorSide
object AnchorSide {
  case object Start extends AnchorSide
  case object End extends AnchorSide
}

case class FilterAnchor(
    position: Position,
    rule: Option[String],
    side: AnchorSide)

object EscapeHatch {
  private val FilterAnchorStart = "\\s?scalafix:off\\s?(.*)".r
  private val FilterAnchorEnd = "\\s?scalafix:on\\s?(.*)".r

  def mkAnchors(
      comment: Token.Comment,
      rules: String,
      side: AnchorSide): List[FilterAnchor] = {
    val splittedRules = rules.split("\\s+").toList
    val position = comment.pos

    if (splittedRules.nonEmpty && splittedRules != List("")) {
      splittedRules
        .map(
          rule =>
            FilterAnchor(
              position,
              Some(rule),
              side
          ))
        .toList
    } else {
      List(
        FilterAnchor(
          position,
          None,
          side
        )
      )
    }
  }

  def apply(tree: Tree): EscapeHatch = {
    val anchors: List[FilterAnchor] =
      tree.tokens
        .collect {
          case comment @ Token.Comment(FilterAnchorStart(rules)) => {
            mkAnchors(comment, rules, AnchorSide.Start)
          }
          case comment @ Token.Comment(FilterAnchorEnd(rules)) => {
            mkAnchors(comment, rules, AnchorSide.End)
          }
        }
        .toList
        .flatten

    val groupAnchors: List[List[List[FilterAnchor]]] = {
      anchors
        .groupBy(_.rule)
        .values
        .map(_.sortBy(_.position.start).grouped(2).toList)
        .toList
    }

    val offBeforeOn =
      "scalafix:off <rule> must appear before scalafix:on <rule>"

    val matchingAnchors: List[Either[AnchorError, CommentFilter]] =
      groupAnchors
        .map(
          _.collect {
            case List(
                FilterAnchor(start, ruleStart, AnchorSide.Start),
                FilterAnchor(end, ruleEnd, AnchorSide.End))
                if ruleStart == ruleEnd => {

              Right(CommentFilter(start, Some(end), ruleStart))
            }

            case List(FilterAnchor(start, rule, AnchorSide.Start)) => {
              Right(CommentFilter(start, None, rule))
            }

            case List(FilterAnchor(end, _, AnchorSide.End)) => {
              Left(AnchorError(offBeforeOn, end))
            }

            case List(
                FilterAnchor(start, rule, AnchorSide.End),
                FilterAnchor(end, _, AnchorSide.Start)) => {

              val range = Position.Range(start.input, start.start, end.end)
              Left(AnchorError(offBeforeOn, range))
            }
          }
        )
        .flatten

    val anchorErrors =
      matchingAnchors.collect { case Left(error) => error }

    val filters =
      matchingAnchors.collect { case Right(filter) => filter }

    new EscapeHatch(filters, anchorErrors)
  }
}

case class AnchorError(msg: String, position: Position)

class EscapeHatch(
    filters: List[CommentFilter],
    val anchorErrors: List[AnchorError]) {
  def isEnabled(ruleName: String, position: Position): Boolean = {
    filters.exists(_.isEnabled(ruleName, position))
  }
}
