package scalafix
package internal.patch

import scalafix.internal.config.FilterMatcher
import scalafix.lint.LintMessage

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.contrib._

import scala.collection.mutable
import scala.collection.immutable.TreeMap

case class CommentFilter(matcher: FilterMatcher, position: Position) {
  def matches(id: String): Boolean =
    matcher.matches(id)
}

/* EscapeHatch is an algorithm to selectively disable rules
 *
 * Rules are disable via comments with a specific syntax
 * scalafix:off or scalafix:on disable or enable rules until the end of file
 * scalafix:ok disable rules on an associated expression
 * a list of rules seperated by commas can be provided to selectively
 * enable or disable rules otherwise all rules are affected
 */
class EscapeHatch(
    enableRules: TreeMap[Int, CommentFilter],
    disableRules: TreeMap[Int, CommentFilter],
    val unusedEnable: List[LintMessage]) {

  // a rule r is disabled in position p if there is a
  // comment disabling r at position p1 < p
  // and there is no comment enabling r in position p2 where p1 < p2 < p.
  private def isEnabled(
      message: LintMessage): (Boolean, Option[CommentFilter]) = {
    var culprit = Option.empty[CommentFilter]
    val isDisabled =
      disableRules.to(message.position.start).exists {
        case (disableOffset, disableFilter) => {
          val isDisabled = disableFilter.matches(message.id)
          if (isDisabled) {
            culprit = Some(disableFilter)
          }
          isDisabled && {
            !enableRules
              .range(disableOffset, message.position.start)
              .values
              .exists { enableFilter =>
                val isEnabled = enableFilter.matches(message.id)
                if (isEnabled) {
                  culprit = Some(enableFilter)
                }
                isEnabled
              }
          }
        }
      }

    (!isDisabled, culprit)
  }

  def filter(lints: List[LintMessage]): List[LintMessage] = {
    val usedEscapes = mutable.Set.empty[CommentFilter]
    val filteredLints = List.newBuilder[LintMessage]

    lints.foreach { lint =>
      val (isLintEnabled, culprit) = isEnabled(lint)

      if (isLintEnabled) {
        filteredLints += lint
      }

      culprit.foreach(usedEscapes += _)
    }

    val escapes = disableRules.values.toSet

    println(
      "disableRules: " + disableRules.values
        .map(v => (v.position.start, v.position.end))
        .toList
        .sorted)
    println(
      "usedEscapes: " + usedEscapes
        .map(v => (v.position.start, v.position.end))
        .toList
        .sorted)

    val unusedDisableWarning =
      EscapeHatch.unusedScalafixSupression(
        "This comment does not disable any rule"
      )

    val unusedEscapesWarning = (escapes -- usedEscapes).toList.map(unused =>
      unusedDisableWarning.at(unused.position))

    filteredLints.result() ++ unusedEnable ++ unusedEscapesWarning
  }
}

object EscapeHatch {

  private val unusedScalafixSupressionId = "UnusedScalafixSupression"

  private[scalafix] def unusedScalafixSupression(explain: String) =
    LintCategory.warning(unusedScalafixSupressionId, explain)

  private val unusedEnableWarning =
    unusedScalafixSupression(
      "This comment would enable a rule that was not disabled (eg: typo in the rules)"
    )

  private sealed abstract class Escape
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

  private def splitRules(rules: String): Set[String] =
    rules.split("\\s+").toSet

  def apply(tree: Tree, associatedComments: AssociatedComments): EscapeHatch = {
    class FastHashComment(comment: Token.Comment) {
      override def hashCode: Int =
        comment.pos.start

      override def equals(other: Any): Boolean =
        comment.eq(other.asInstanceOf[Object])
    }

    val escapes = List.newBuilder[Escape]
    var currentlyDisabledRules = Set.empty[String]
    val unusedEnable = List.newBuilder[LintMessage]
    val visitedFilterExpression = mutable.Set.empty[FastHashComment]

    def addExpression(t: Tree)(comment: Token.Comment): Unit = comment match {
      case Token.Comment(FilterExpression(rules)) =>
        escapes += Escape.Expression(t.pos, rules)
        visitedFilterExpression += new FastHashComment(comment)

      case _ => ()
    }

    tree.foreach { t =>
      associatedComments.trailing(t).foreach(addExpression(t))
      associatedComments.leading(t).foreach(addExpression(t))
    }

    tree.tokens.foreach {
      case comment @ Token.Comment(FilterDisable(rules)) =>
        escapes += Escape.UntilEOF(comment.pos, rules, Toogle.Disable)
        currentlyDisabledRules = currentlyDisabledRules ++ splitRules(rules)

      case comment @ Token.Comment(FilterEnable(rules)) =>
        escapes += Escape.UntilEOF(comment.pos, rules, Toogle.Enable)

        val enabledRules = splitRules(rules)
        val enabledNotDisabled = enabledRules -- currentlyDisabledRules
        enabledNotDisabled.foreach(rule =>
          unusedEnable += unusedEnableWarning.at(comment.pos))

        val reEnabled = currentlyDisabledRules -- enabledRules

      case comment @ Token.Comment(FilterExpression(rules)) =>
        // At the end of object/trait/class the AssociatedComments is
        // not trailing or leading for example:
        //
        // object Dummy { // scalafix:ok EscapeHatchDummyLinter
        //   1
        // }
        // we just keep track of visited Escape.Expression

        val fastComment = new FastHashComment(comment)
        if (!visitedFilterExpression.contains(fastComment)) {
          escapes += Escape.Expression(comment.pos, rules)
        }

      case _ => ()
    }

    EscapeHatch(escapes.result(), unusedEnable.result())
  }

  private def apply(
      comments: List[Escape],
      unusedEnable: List[LintMessage]): EscapeHatch = {
    val enableRules = TreeMap.newBuilder[Int, CommentFilter]
    val disableRules = TreeMap.newBuilder[Int, CommentFilter]

    def matcher(rules: String): FilterMatcher = {
      if (rules.isEmpty) {
        FilterMatcher.matchEverything
      } else {
        FilterMatcher(
          includes = splitRules(rules).toSeq,
          excludes = Seq()
        )
      }
    }

    def enable(offset: Int, position: Position, rules: String): Unit = {
      enableRules += (offset -> CommentFilter(matcher(rules), position))
    }

    def disable(offset: Int, position: Position, rules: String): Unit = {
      disableRules += (offset -> CommentFilter(matcher(rules), position))
    }

    comments.foreach {
      case Escape.Expression(pos, rules) =>
        // capture from the beginning of the line
        val position =
          Position.Range(
            pos.input,
            pos.start - pos.startColumn,
            pos.end
          )

        disable(position.start, pos, rules)
        enable(position.end, pos, rules)

      case Escape.UntilEOF(position, rules, toogle) =>
        toogle match {
          case Toogle.Disable =>
            disable(position.start, position, rules)

          case Toogle.Enable =>
            enable(position.end, position, rules)
        }
    }

    new EscapeHatch(
      enableRules = enableRules.result(),
      disableRules = disableRules.result(),
      unusedEnable = unusedEnable
    )
  }
}
