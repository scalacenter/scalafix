package scalafix
package internal.patch

import scalafix.internal.config.FilterMatcher
import scalafix.lint.LintMessage
import scalafix.rule.RuleName
import scalafix.patch._
import scalafix.internal.diff.DiffDisable

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.contrib._

import scala.collection.mutable
import scala.collection.immutable.TreeMap

/* EscapeHatch is an algorithm to selectively disable rules
 *
 * Rules are disabled via comments with a specific syntax
 * scalafix:off or scalafix:on disable or enable rules until the end of file
 * scalafix:ok disable rules on an associated expression
 * a list of rules seperated by commas can be provided to selectively
 * enable or disable rules otherwise all rules are affected
 *
 * enableRules and disableRules contains the offset at which you
 * start applying a filter
 *
 * unusedEnable contains the unused scalafix:on
 */
class EscapeHatch(
    enableRules: TreeMap[EscapeHatch.EscapeOffset, EscapeHatch.EscapeFilter],
    disableRules: TreeMap[EscapeHatch.EscapeOffset, EscapeHatch.EscapeFilter],
    unusedEnable: List[LintMessage]) {

  import EscapeHatch._

  // a rule r is disabled in position p if there is a
  // comment disabling r at position p1 < p
  // and there is no comment enabling r in position p2 where p1 < p2 < p.
  private def isEnabled(
      name: String,
      start: Int): (Boolean, Option[EscapeFilter]) = {
    var culprit = Option.empty[EscapeFilter]

    val isDisabled =
      disableRules.to(EscapeOffset(start)).exists {
        case (disableOffset, disableFilter) => {
          val isDisabled = disableFilter.matches(name)
          if (isDisabled) {
            culprit = Some(disableFilter)
          }
          isDisabled && {
            !enableRules
              .range(disableOffset, EscapeOffset(start))
              .values
              .exists { enableFilter =>
                val isEnabled = enableFilter.matches(name)
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

  def filter(
      patchesByName: Map[RuleName, Patch],
      ctx: RuleCtx,
      index: SemanticdbIndex,
      diff: DiffDisable): (Patch, List[LintMessage]) = {
    val usedEscapes = mutable.Set.empty[EscapeOffset]
    val lintMessages = List.newBuilder[LintMessage]

    def disabledByEscape(name: String, start: Int): Boolean = {
      // check if part of on/off/ok blocks
      val (isPatchEnabled, culprit) = isEnabled(name, start)
      // to track unused on/off/ok
      culprit.foreach(escape => usedEscapes += escape.offset)
      !isPatchEnabled
    }

    def loop(name: RuleName, patch: Patch): Patch = patch match {
      case AtomicPatch(underlying) =>
        val hasDisabledPatch = {
          val patches = Patch.treePatchApply(underlying)(ctx, index)
          patches.exists { tp =>
            val byGit = diff.isDisabled(tp.tok.pos)
            val byEscape = disabledByEscape(name.toString, tp.tok.pos.start)
            byGit || byEscape
          }
        }

        if (hasDisabledPatch) EmptyPatch
        else loop(name, underlying)

      case Concat(a, b) =>
        Concat(loop(name, a), loop(name, b))

      case LintPatch(orphanLint) =>
        val lint = orphanLint.withOwner(name)

        val byGit = diff.isDisabled(lint.position)
        val byEscape = disabledByEscape(lint.id, lint.position.start)

        val isLintDisabled = byGit || byEscape

        if (!isLintDisabled) {
          lintMessages += lint
        }

        EmptyPatch

      case e => e
    }

    val patches =
      patchesByName.map {
        case (name, patch) =>
          loop(name, patch)
      }.asPatch

    val unusedDisableWarning =
      LintCategory
        .warning(
          "Disable",
          "This comment does not disable any rule"
        )
        .withOwner(unusedScalafixSupression)

    val unusedEscapesWarning =
      (disableRules -- usedEscapes).values
        .map(unused => unusedDisableWarning.at(unused.anchorPosition.value))

    val filteredLints = lintMessages.result()

    (patches, filteredLints ++ unusedEnable ++ unusedEscapesWarning)
  }
}

object EscapeHatch {
  private[EscapeHatch] case class EscapeFilter(
      matcher: FilterMatcher,
      anchorPosition: AnchorPosition,
      offset: EscapeOffset
  ) {
    def matches(id: String): Boolean =
      matcher.matches(id)
  }

  // escape offset are the offset at witch a filter is effective
  private[EscapeHatch] case class EscapeOffset(offset: Int)

  object EscapeOffset {
    implicit val ordering: Ordering[EscapeOffset] = Ordering.by(_.offset)
  }

  // anchors are comments containing scalafix:[on|off|ok]
  private[EscapeHatch] case class AnchorPosition(value: Position) {
    override def hashCode: Int = value.start
  }

  private[EscapeHatch] case class ExpressionPosition(value: Position) {
    override def hashCode: Int = value.start
  }

  private sealed abstract class Escape
  private object Escape {
    case class UntilEOF(
        anchorPosition: AnchorPosition,
        rules: String,
        toogle: Toogle
    ) extends Escape

    case class Expression(
        expressionPosition: ExpressionPosition,
        anchorPosition: AnchorPosition,
        rules: String
    ) extends Escape

    case class ExpressionLine(
        expressionPosition: ExpressionPosition,
        anchorPosition: AnchorPosition,
        rules: String
    ) extends Escape
  }

  private sealed abstract class Toogle
  private object Toogle {
    case object Disable extends Toogle
    case object Enable extends Toogle
  }

  private val unusedScalafixSupression = RuleName("UnusedScalafixSupression")

  private val unusedEnableWarning =
    LintCategory
      .warning(
        "Enable",
        "This comment would enable a rule that was not disabled (eg: typo in the rules)"
      )
      .withOwner(unusedScalafixSupression)

  private val FilterDisable = "\\s?scalafix:off\\s?(.*)".r
  private val FilterEnable = "\\s?scalafix:on\\s?(.*)".r
  private val FilterExpression = "\\s?scalafix:ok\\s?(.*)".r

  private def splitRules(rules: String): Set[String] =
    rules.split("\\s+").toSet

  def apply(tree: Tree, associatedComments: AssociatedComments): EscapeHatch = {
    val escapes = List.newBuilder[Escape]
    val unusedEnable = List.newBuilder[LintMessage]
    val visitedFilterExpression = mutable.Set.empty[AnchorPosition]

    var currentlyDisabledRules = Set.empty[String]
    def trackUnusedRules(
        rules: String,
        toogle: Toogle,
        anchorPosition: AnchorPosition): Unit = {
      toogle match {
        case Toogle.Disable =>
          currentlyDisabledRules = currentlyDisabledRules ++ splitRules(rules)

        case Toogle.Enable =>
          val enabledRules = splitRules(rules)
          val enabledNotDisabled = enabledRules -- currentlyDisabledRules

          enabledNotDisabled.foreach(
            rule => unusedEnable += unusedEnableWarning.at(anchorPosition.value)
          )
      }
    }

    tree.foreach { t =>
      associatedComments.trailing(t).foreach {
        // matches simple expressions
        //
        // val a = (
        //   1,
        //   2
        // ) // scalafix:ok RuleA
        //
        case comment @ Token.Comment(FilterExpression(rules)) =>
          val anchorPosition = AnchorPosition(comment.pos)
          if (!visitedFilterExpression.contains(anchorPosition)) {
            escapes += Escape.Expression(
              ExpressionPosition(t.pos),
              anchorPosition,
              rules
            )
            visitedFilterExpression += anchorPosition
          }

        case _ => ()
      }
    }

    tree.tokens.foreach {
      case comment @ Token.Comment(rawComment) => {
        rawComment match {
          // matches off anchors
          //
          // // scalafix:off RuleA
          // ...
          //
          case FilterDisable(rules) => {
            val position = AnchorPosition(comment.pos)
            escapes += Escape.UntilEOF(position, rules, Toogle.Disable)
            trackUnusedRules(rules, Toogle.Disable, position)
          }

          // matches on anchors
          //
          // ...
          // // scalafix:on RuleA
          //
          case FilterEnable(rules) => {
            val position = AnchorPosition(comment.pos)
            escapes += Escape.UntilEOF(position, rules, Toogle.Enable)
            trackUnusedRules(rules, Toogle.Enable, position)
          }
          // matches expressions not handled by AssociatedComments
          //
          // object Dummy { // scalafix:ok EscapeHatchDummyLinter
          //   1
          // }
          case FilterExpression(rules) => {
            val anchorPosition = AnchorPosition(comment.pos)
            if (!visitedFilterExpression.contains(anchorPosition)) {
              // we approximate the position of the expression to the whole line
              val expressionPosition =
                ExpressionPosition(
                  Position.Range(
                    comment.pos.input,
                    comment.pos.start - comment.pos.startColumn,
                    comment.pos.end
                  ))

              escapes += Escape.ExpressionLine(
                expressionPosition = expressionPosition,
                anchorPosition = anchorPosition,
                rules = rules
              )
            }
          }
          case _ => ()
        }

      }
      case _ => ()
    }

    EscapeHatch(escapes.result(), unusedEnable.result())
  }

  private def matcher(rules: String): FilterMatcher = {
    if (rules.isEmpty) {
      FilterMatcher.matchEverything
    } else {
      FilterMatcher(
        includes = splitRules(rules).toSeq,
        excludes = Seq()
      )
    }
  }

  private def apply(
      comments: List[Escape],
      unusedEnable: List[LintMessage]): EscapeHatch = {
    val enableRules = TreeMap.newBuilder[EscapeOffset, EscapeFilter]
    val disableRules = TreeMap.newBuilder[EscapeOffset, EscapeFilter]

    def enable(
        offset: EscapeOffset,
        anchor: AnchorPosition,
        rules: String): Unit = {
      enableRules += (offset -> EscapeFilter(matcher(rules), anchor, offset))
    }

    def disable(
        offset: EscapeOffset,
        anchor: AnchorPosition,
        rules: String): Unit = {
      disableRules += (offset -> EscapeFilter(matcher(rules), anchor, offset))
    }

    comments.foreach {
      case Escape.Expression(expressionPos, anchorPos, rules) =>
        disable(EscapeOffset(expressionPos.value.start), anchorPos, rules)
        enable(EscapeOffset(expressionPos.value.end), anchorPos, rules)

      case Escape.ExpressionLine(expressionPos, anchorPos, rules) =>
        disable(EscapeOffset(expressionPos.value.start), anchorPos, rules)
        enable(EscapeOffset(expressionPos.value.end), anchorPos, rules)

      case Escape.UntilEOF(anchorPos, rules, toogle) =>
        toogle match {
          case Toogle.Disable =>
            disable(EscapeOffset(anchorPos.value.start), anchorPos, rules)

          case Toogle.Enable =>
            enable(EscapeOffset(anchorPos.value.end), anchorPos, rules)
        }
    }

    new EscapeHatch(
      enableRules = enableRules.result(),
      disableRules = disableRules.result(),
      unusedEnable = unusedEnable
    )
  }
}
