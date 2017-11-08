package scalafix
package internal.patch

import scalafix.internal.config.FilterMatcher
import scalafix.lint.LintMessage

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.contrib._

import scala.collection.mutable
import scala.collection.immutable.TreeMap

/* EscapeHatch is an algorithm to selectively disable rules
 *
 * Rules are disable via comments with a specific syntax
 * scalafix:off or scalafix:on disable or enable rules until the end of file
 * scalafix:ok disable rules on an associated expression
 * a list of rules seperated by commas can be provided to selectively
 * enable or disable rules otherwise all rules are affected
 */
class EscapeHatch(
    enableRules: TreeMap[Int, FilterMatcher],
    disableRules: TreeMap[Int, FilterMatcher],
    val unusedEscapes: List[LintMessage]) {


  def filter(lints: List[LintMessage]): List[LintMessage] = {
    lints.filterNot(isDisabled) ++ unusedEscapes
  }

  // a rule r is disabled in position p if there is a
  // comment disabling r at position p1 < p
  // and there is no comment enabling r in position p2 where p1 < p2 < p.
  private def isDisabled(message: LintMessage): Boolean = {
    disableRules.to(message.position.start).exists {
      case (disablePosition, disableRule) => {
        disableRule.matches(message.id) && {
          !enableRules
            .range(disablePosition, message.position.start)
            .values
            .exists(
              _.matches(message.id)
            )
        }
      }
    }
  }
}

object EscapeHatch {
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
    val unusedEscapes = List.newBuilder[LintMessage]
    val unusedScalafixComment = LintCategory.error("UnusedScalafixSupression", "exp")
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
          unusedEscapes += unusedScalafixComment.at(comment.pos)
        )

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

          val pos =
            Position.Range(
              comment.pos.input,
              comment.pos.start - comment.pos.startColumn,
              comment.pos.end
            )

          escapes += Escape.Expression(pos, rules)
        }

      case _ => ()
    }

    EscapeHatch(escapes.result(), unusedEscapes.result())
  }

  private def apply(
      comments: List[Escape],
      unusedEscapes: List[LintMessage]): EscapeHatch = {
    val enableRules = TreeMap.newBuilder[Int, FilterMatcher]
    val disableRules = TreeMap.newBuilder[Int, FilterMatcher]

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

    comments.foreach {
      case Escape.Expression(position, rules) =>
        val ruleMatcher = matcher(rules)
        disableRules += (position.start -> ruleMatcher)
        enableRules += (position.end -> ruleMatcher)

      case Escape.UntilEOF(position, rules, toogle) =>
        val toogles =
          toogle match {
            case Toogle.Disable => disableRules
            case Toogle.Enable => enableRules
          }
        toogles += (position.start -> matcher(rules))
    }

    new EscapeHatch(
      enableRules = enableRules.result(),
      disableRules = disableRules.result(),
      unusedEscapes = unusedEscapes
    )
  }
}
