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
import scalafix.internal.patch.EscapeHatch._

// TODO update scaladocs
/** EscapeHatch is an algorithm to selectively disable rules
  *
  * Rules are disabled via comments with a specific syntax
  * `scalafix:off` or `scalafix:on` disable or enable rules until the end of file
  * `scalafix:ok` disable rules on an associated expression
  * a list of rules separated by commas can be provided to selectively
  * enable or disable rules otherwise all rules are affected
  *
  * enableRules and disableRules contains the offset at which you
  * start applying a filter
  *
  * unusedEnable contains the unused `scalafix:on`
  */
class EscapeHatch private (
    anchoredEscapes: AnchoredEscapes,
    annotatedEscapes: AnnotatedEscapes) {

  def filter(
      patchesByName: Map[RuleName, Patch],
      ctx: RuleCtx,
      index: SemanticdbIndex,
      diff: DiffDisable): (Patch, List[LintMessage]) = {
    val usedEscapes = mutable.Set.empty[EscapeOffset]
    val lintMessages = List.newBuilder[LintMessage]

    def disabledByEscape(name: RuleName, start: Int): Boolean =
      annotatedEscapes.isEnabled(name, start) match {
        case (false, Some(culprit)) => true // TODO keep track of culprit
        case _ =>
          // check if part of on/off/ok blocks
          val (enabled, culprit) = anchoredEscapes.isEnabled(name, start)
          // to track unused on/off/ok
          culprit.foreach(escape => usedEscapes += escape.startOffset)
          !enabled
      }

    def loop(name: RuleName, patch: Patch): Patch = patch match {
      case AtomicPatch(underlying) =>
        val hasDisabledPatch = {
          val patches = Patch.treePatchApply(underlying)(ctx, index)
          patches.exists { tp =>
            val byGit = diff.isDisabled(tp.tok.pos)
            val byEscape = disabledByEscape(name, tp.tok.pos.start)
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
        .withOwner(UnusedScalafixSuppression)

    val unusedEscapesWarning =
      (anchoredEscapes.disableRules -- usedEscapes).values
        .map(unused => unusedDisableWarning.at(unused.escapePosition))

    val filteredLints = lintMessages.result()

    (
      patches,
      filteredLints ++ anchoredEscapes.unusedEnable ++ unusedEscapesWarning)
  }
}

// TODO visibility of members
object EscapeHatch {

  case class EscapeFilter(
      matcher: FilterMatcher,
      escapePosition: Position,
      startOffset: EscapeOffset,
      endOffset: Option[EscapeOffset] = None) {
    def matches(id: RuleName): Boolean =
      id.identifiers.exists(i => matcher.matches(i.value))
  }

  case class EscapeOffset(offset: Int)

  object EscapeOffset {
    implicit val ordering: Ordering[EscapeOffset] = Ordering.by(_.offset)
  }

  private val UnusedScalafixSuppression = RuleName("UnusedScalafixSuppression")

  private val UnusedEnableWarning = LintCategory
    .warning(
      "Enable",
      "This comment would enable a rule that was not disabled (eg: typo in the rules)"
    )
    .withOwner(UnusedScalafixSuppression)

  def splitRules(rules: String): Set[String] = rules.split("\\s+").toSet

  def apply(tree: Tree, associatedComments: AssociatedComments): EscapeHatch =
    new EscapeHatch(
      AnchoredEscapes(tree, associatedComments),
      AnnotatedEscapes(tree)
    )

  def matcher(rules: String): FilterMatcher =
    if (rules.isEmpty) FilterMatcher.matchEverything
    else
      FilterMatcher(
        includes = splitRules(rules).toSeq,
        excludes = Seq()
      )

  /**
    * TODO Rules from SuppressWarnings annotations.
    */
  class AnnotatedEscapes private (
      val escapeRules: TreeMap[EscapeOffset, EscapeFilter]) {

    // TODO support 'all'
    // TODO support rules with 'scalafix:' prefix
    def isEnabled(
        ruleName: RuleName,
        position: Int): (Boolean, Option[EscapeFilter]) = {
      val maybeDisabled = escapeRules.to(EscapeOffset(position)).find {
        case (_, f @ EscapeFilter(_, _, _, Some(to)))
            if f.matches(ruleName) && to.offset >= position =>
          true
        case _ => false
      }

      maybeDisabled match {
        case Some((_, filter)) => (false, Some(filter))
        case None => (true, None)
      }
    }
  }

  object AnnotatedEscapes {
    private val SuppressWarnings = "SuppressWarnings"

    def apply(tree: Tree): AnnotatedEscapes = {
      val builder = TreeMap.newBuilder[EscapeOffset, EscapeFilter]

      def addAnnotatedEscape(t: Tree, mods: List[Mod]): Unit = {
        val startOffset = EscapeOffset(t.pos.start)
        val endOffset = EscapeOffset(t.pos.end)
        val ruleMatcher = matcher(extractRules(mods).mkString(" "))
        builder += (startOffset -> EscapeFilter(
          ruleMatcher,
          t.pos, // TODO exact position of the annotation
          startOffset,
          Some(endOffset)))
      }

      def hasSuppressWarnings(mods: List[Mod]): Boolean =
        mods.exists {
          case Mod.Annot(Init(Type.Name(SuppressWarnings), _, _)) => true
          case _ => false
        }

      def extractRules(mods: List[Mod]): Set[String] =
        mods.flatMap {
          case Mod.Annot(
              Init(
                Type.Name(SuppressWarnings),
                _,
                (Term.Apply(Term.Name("Array"), rules) :: Nil) :: Nil)) =>
            rules.map { case Lit.String(rule) => rule }

          case _ => Nil
        }.toSet

      tree.foreach {
        case t @ Defn.Class(mods, _, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Defn.Object(mods, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Defn.Trait(mods, _, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Defn.Type(mods, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Defn.Def(mods, _, _, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Defn.Val(mods, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Defn.Var(mods, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Term.Param(mods, _, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Ctor.Primary(mods, _, _) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case t @ Ctor.Secondary(mods, _, _, _, _)
            if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)

        case _ => ()
      }

      new AnnotatedEscapes(builder.result())
    }
  }

  /**
    * TODO Rules from comments.
    */
  class AnchoredEscapes private (
      val enableRules: TreeMap[EscapeOffset, EscapeFilter],
      val disableRules: TreeMap[EscapeOffset, EscapeFilter],
      val unusedEnable: List[LintMessage]) {

    /**
      * a rule r is disabled in position p if there is a comment disabling r at
      * position p1 < p and there is no comment enabling r in position p2 where p1 < p2 < p.
      */
    def isEnabled(
        ruleName: RuleName,
        position: Int): (Boolean, Option[EscapeFilter]) = {
      var culprit = Option.empty[EscapeFilter]

      val isDisabled =
        disableRules.to(EscapeOffset(position)).exists {
          case (disableOffset, disableFilter) => {
            val isDisabled = disableFilter.matches(ruleName)
            if (isDisabled) culprit = Some(disableFilter)

            isDisabled && {
              !enableRules
                .range(disableOffset, EscapeOffset(position))
                .values
                .exists { enableFilter =>
                  val isEnabled = enableFilter.matches(ruleName)
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
  }

  object AnchoredEscapes {
    private val FilterDisable = "\\s?scalafix:off\\s?(.*)".r
    private val FilterEnable = "\\s?scalafix:on\\s?(.*)".r
    private val FilterExpression = "\\s?scalafix:ok\\s?(.*)".r

    def apply(
        tree: Tree,
        associatedComments: AssociatedComments): AnchoredEscapes = {
      val enableBuilder = TreeMap.newBuilder[EscapeOffset, EscapeFilter]
      val disableBuilder = TreeMap.newBuilder[EscapeOffset, EscapeFilter]
      val unusedAnchoredEnable = List.newBuilder[LintMessage]
      val visitedFilterExpression = mutable.Set.empty[Position]
      var currentlyDisabledRules = Set.empty[String]

      def enable(
          offset: EscapeOffset,
          anchor: Position,
          rules: String): Unit = {
        val filter = EscapeFilter(matcher(rules), anchor, offset)
        enableBuilder += (offset -> filter)
      }

      def disable(
          offset: EscapeOffset,
          anchor: Position,
          rules: String): Unit = {
        val filter = EscapeFilter(matcher(rules), anchor, offset)
        disableBuilder += (offset -> filter)
      }

      def trackUnusedRules(
          anchorPos: Position,
          rules: String,
          enabled: Boolean): Unit =
        if (enabled) {
          val enabledRules = splitRules(rules)
          val enabledNotDisabled = enabledRules -- currentlyDisabledRules

          enabledNotDisabled.foreach(
            _ =>
              unusedAnchoredEnable += UnusedEnableWarning
                .at(anchorPos) // TODO unusedEnableWarning
          )
        } else {
          currentlyDisabledRules = currentlyDisabledRules ++ splitRules(rules)
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
            val anchorPos = comment.pos
            if (!visitedFilterExpression.contains(anchorPos)) {
              disable(EscapeOffset(t.pos.start), anchorPos, rules)
              enable(EscapeOffset(t.pos.end), anchorPos, rules)
              visitedFilterExpression += anchorPos
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
              val anchorPos = comment.pos
              disable(EscapeOffset(anchorPos.start), anchorPos, rules)
              trackUnusedRules(anchorPos, rules, enabled = false)
            }

            // matches on anchors
            //
            // ...
            // // scalafix:on RuleA
            //
            case FilterEnable(rules) => {
              val anchorPos = comment.pos
              enable(EscapeOffset(anchorPos.start), anchorPos, rules)
              trackUnusedRules(anchorPos, rules, enabled = true)
            }

            // matches expressions not handled by AssociatedComments
            //
            // object Dummy { // scalafix:ok EscapeHatchDummyLinter
            //   1
            // }
            case FilterExpression(rules) => {
              val anchorPos = comment.pos
              if (!visitedFilterExpression.contains(anchorPos)) {
                // we approximate the position of the expression to the whole line
                val position = Position.Range(
                  anchorPos.input,
                  anchorPos.start - anchorPos.startColumn,
                  anchorPos.end
                )
                disable(EscapeOffset(position.start), anchorPos, rules)
                enable(EscapeOffset(position.end), anchorPos, rules)
              }
            }

            case _ => ()
          }

        }
        case _ => ()
      }

      new AnchoredEscapes(
        enableBuilder.result(),
        disableBuilder.result(),
        unusedAnchoredEnable.result())
    }
  }
}
