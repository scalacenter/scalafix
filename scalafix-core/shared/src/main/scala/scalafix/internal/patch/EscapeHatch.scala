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
import scala.collection.mutable.ListBuffer
import scalafix.internal.patch.EscapeHatch._

/** EscapeHatch is an algorithm to selectively disable rules. There
  * are two mechanisms to do so: anchored comments and the
  * standard `@SuppressWarnings` annotation. The latter takes
  * precedence over the former in case there are overlaps.
  * See `AnchoredEscapes` and `AnnotatedEscapes` for more details.
  */
class EscapeHatch private (
    anchoredEscapes: AnchoredEscapes,
    annotatedEscapes: AnnotatedEscapes) {

  def filter(
      patchesByName: Map[RuleName, Patch],
      ctx: RuleCtx,
      index: SemanticdbIndex,
      diff: DiffDisable): (Patch, List[LintMessage]) = {
    val usedEscapes = mutable.Set.empty[EscapeFilter]
    val lintMessages = List.newBuilder[LintMessage]

    def disabledByEscape(name: RuleName, start: Int): Boolean =
      annotatedEscapes.isEnabled(name, start) match {
        case (false, Some(culprit)) => true // TODO keep track of culprit
        case _ =>
          // check if part of on/off/ok blocks
          val (enabled, culprit) = anchoredEscapes.isEnabled(name, start)
          // to track unused on/off/ok
          culprit.foreach(escape => usedEscapes += escape)
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
      patchesByName.map { case (name, patch) => loop(name, patch) }.asPatch

    val unusedDisableWarning = LintCategory
      .warning(
        "Disable",
        "Rule(s) don't need to be disabled at this position. This can be removed!")
      .withOwner(UnusedScalafixSuppression)
    val unusedEnableWarning = LintCategory
      .warning(
        "Enable",
        "Rule(s) not disabled at this position. This can be removed!")
      .withOwner(UnusedScalafixSuppression)

    // TODO warn unused only for rules with the 'scalafix:' prefix
    val unusedDisableEscapeWarning =
      anchoredEscapes.disableEscapes
        .filterNot(usedEscapes)
        .map(unused => unusedDisableWarning.at(unused.source))
    val unusedEnableEscapeWarnings =
      anchoredEscapes.unusedEnable.map(anchorPos =>
        unusedEnableWarning.at(anchorPos))
    val warnings =
      lintMessages.result() ++
        unusedEnableEscapeWarnings ++
        unusedDisableEscapeWarning

    (patches, warnings)
  }
}

// TODO visibility of members
object EscapeHatch {

  private val UnusedScalafixSuppression = RuleName("UnusedScalafixSuppression")

  private type EscapeTree = TreeMap[EscapeOffset, List[EscapeFilter]]

  case class EscapeFilter(
      matcher: FilterMatcher,
      source: Position,
      startOffset: EscapeOffset,
      endOffset: Option[EscapeOffset] = None) {
    def matches(id: RuleName): Boolean =
      id.identifiers.exists(i => matcher.matches(i.value))
  }

  case class EscapeOffset(offset: Int)

  object EscapeOffset {
    implicit val ordering: Ordering[EscapeOffset] = Ordering.by(_.offset)
  }

  def apply(tree: Tree, associatedComments: AssociatedComments): EscapeHatch =
    new EscapeHatch(
      AnchoredEscapes(tree, associatedComments),
      AnnotatedEscapes(tree)
    )

  /** Rules are disabled via standard `@SuppressWarnings` annotations. The
    * annotation can be placed in class, object, trait, type, def, val, var,
    * parameters and constructor definitions.
    *
    * Rules can be optionally prefixed with `scalafix:`. Besides helping to
    * users to understand where the rules are coming from, it also allows
    * Scalafix to warn when there are rules unnecessarily being suppressed.
    *
    * Use the keyword "all" to suppress all rules.
    */
  class AnnotatedEscapes private (escapeTree: EscapeTree) {

    def isEnabled(
        ruleName: RuleName,
        position: Int): (Boolean, Option[EscapeFilter]) = {
      val escapesUpToPos = escapeTree.to(EscapeOffset(position)).values.flatten
      val maybeFilter = escapesUpToPos.find {
        case f @ EscapeFilter(_, _, _, Some(end))
            if f.matches(ruleName) && end.offset >= position =>
          true
        case _ => false
      }
      maybeFilter match {
        case Some(filter) => (false, Some(filter))
        case None => (true, None)
      }
    }
  }

  object AnnotatedEscapes {
    private val SuppressWarnings = "SuppressWarnings"
    private val SuppressAll = "all"
    private val OptionalRulePrefix = "scalafix:"

    def apply(tree: Tree): AnnotatedEscapes = {
      val builder = TreeMap.newBuilder[EscapeOffset, List[EscapeFilter]]

      def hasSuppressWarnings(mods: List[Mod]): Boolean =
        mods.exists {
          case Mod.Annot(Init(Type.Name(SuppressWarnings), _, _)) => true
          case _ => false
        }

      def addAnnotatedEscape(t: Tree, mods: List[Mod]): Unit = {
        val start = EscapeOffset(t.pos.start)
        val end = EscapeOffset(t.pos.end)
        val rules = extractRules(mods)
        val (matchAll, matchOne) = rules.partition(_._1 == SuppressAll)
        val filters = ListBuffer.empty[EscapeFilter]

        // 'all' should come before individual rules so that we can warn unused rules later
        for ((_, rulePos) <- matchAll) {
          val matcher = FilterMatcher.matchEverything
          filters += EscapeFilter(matcher, rulePos, start, Some(end))
        }
        for ((rule, rulePos) <- matchOne) {
          val unprefixedRuleName = rule.stripPrefix(OptionalRulePrefix)
          val matcher = FilterMatcher(unprefixedRuleName)
          filters += EscapeFilter(matcher, rulePos, start, Some(end))
        }

        builder += (start -> filters.result())
      }

      def extractRules(mods: List[Mod]): List[(String, Position)] =
        mods.flatMap {
          case Mod.Annot(
              Init(
                Type.Name(SuppressWarnings),
                _,
                List(Term.Apply(Term.Name("Array"), rules) :: Nil))) =>
            rules.map { case lit @ Lit.String(rule) => (rule, lit.pos) }

          case _ => Nil
        }

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

  /** Rules are disabled via comments with a specific syntax:
    * `scalafix:off` or `scalafix:on` disable or enable rules until the end of file
    * `scalafix:ok` disable rules on an associated expression
    * a list of rules separated by commas can be provided to selectively
    * enable or disable rules otherwise all rules are affected
    *
    * `enabling` and `enabling` contain the offset at which you start applying a filter
    *
    * `unusedEnable` contains the unused `scalafix:on`
    */
  class AnchoredEscapes private (
      enabling: EscapeTree,
      disabling: EscapeTree,
      val unusedEnable: List[Position]) {

    /**
      * a rule r is disabled in position p if there is a comment disabling r at
      * position p1 < p and there is no comment enabling r in position p2 where p1 < p2 < p.
      */
    def isEnabled(
        ruleName: RuleName,
        position: Int): (Boolean, Option[EscapeFilter]) = {
      var culprit = Option.empty[EscapeFilter]
      val isDisabled = {
        val disablesUpToPos =
          disabling.to(EscapeOffset(position)).values.flatten
        disablesUpToPos.exists { disableFilter =>
          val isDisabled = disableFilter.matches(ruleName)
          if (isDisabled) culprit = Some(disableFilter)

          isDisabled && {
            val enablesRange = enabling
              .range(disableFilter.startOffset, EscapeOffset(position))
              .values
              .flatten
            !enablesRange.exists { enableFilter =>
              val isEnabled = enableFilter.matches(ruleName)
              if (isEnabled) culprit = Some(enableFilter)
              isEnabled
            }
          }
        }
      }

      (!isDisabled, culprit)
    }

    def disableEscapes: Iterable[EscapeFilter] = disabling.values.flatten
  }

  object AnchoredEscapes {
    private val FilterDisable = "\\s?scalafix:off\\s?(.*)".r
    private val FilterEnable = "\\s?scalafix:on\\s?(.*)".r
    private val FilterExpression = "\\s?scalafix:ok\\s?(.*)".r

    def apply(
        tree: Tree,
        associatedComments: AssociatedComments): AnchoredEscapes = {
      val enableBuilder = TreeMap.newBuilder[EscapeOffset, List[EscapeFilter]]
      val disableBuilder = TreeMap.newBuilder[EscapeOffset, List[EscapeFilter]]
      val unusedAnchoredEnable = List.newBuilder[Position]
      val visitedFilterExpression = mutable.Set.empty[Position]
      var currentlyDisabledRules = Map.empty[String, Position]

      def enable(
          offset: EscapeOffset,
          anchor: Token.Comment,
          rules: String): Unit =
        enableBuilder += (offset -> filtersFor(offset, anchor, rules))

      def disable(
          offset: EscapeOffset,
          anchor: Token.Comment,
          rules: String): Unit =
        disableBuilder += (offset -> filtersFor(offset, anchor, rules))

      def filtersFor(
          offset: EscapeOffset,
          anchor: Token.Comment,
          rules: String): List[EscapeFilter] = {
        val splittedRules = splitRules(rules)

        if (splittedRules.isEmpty) { // wildcard
          List(EscapeFilter(FilterMatcher.matchEverything, anchor.pos, offset))
        } else {
          rulesExactPosition(splittedRules, anchor)
            .map {
              case (rule, pos) =>
                EscapeFilter(FilterMatcher(rule), pos, offset)
            }
        }
      }

      def splitRules(rules: String): List[String] =
        rules.trim.split(",\\s*").toList

      def rulesExactPosition(
          rules: List[String],
          anchor: Token.Comment): List[(String, Position)] = {
        val rulesToPos = ListBuffer.empty[(String, Position)]
        var fromIdx = 0

        for (rule <- rules) {
          val idx = anchor.text.indexOf(rule, fromIdx)
          val startPos = anchor.start + idx
          val endPos = startPos + rule.length
          val pos = Position.Range(anchor.input, startPos, endPos)
          fromIdx = idx + rule.length
          rulesToPos += (rule -> pos)
        }
        rulesToPos.result()
      }

      def trackUnusedRules(
          anchor: Token.Comment,
          rules: String,
          enabled: Boolean): Unit = {
        val rulesToPos = rulesExactPosition(splitRules(rules), anchor)

        if (enabled) {
          if (currentlyDisabledRules.isEmpty && rulesToPos.isEmpty) {
            unusedAnchoredEnable += anchor.pos
          } else {
            val disabledRules = currentlyDisabledRules.keySet
            rulesToPos.foreach {
              case (rule, pos) if !disabledRules(rule) =>
                unusedAnchoredEnable += pos
            }
          }
        } else {
          currentlyDisabledRules ++= rulesToPos
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
            if (!visitedFilterExpression.contains(comment.pos)) {
              disable(EscapeOffset(t.pos.start), comment, rules)
              enable(EscapeOffset(t.pos.end), comment, rules)
              visitedFilterExpression += comment.pos
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
              disable(EscapeOffset(comment.pos.start), comment, rules)
              trackUnusedRules(comment, rules, enabled = false)
            }

            // matches on anchors
            //
            // ...
            // // scalafix:on RuleA
            //
            case FilterEnable(rules) => {
              enable(EscapeOffset(comment.pos.start), comment, rules)
              trackUnusedRules(comment, rules, enabled = true)
            }

            // matches expressions not handled by AssociatedComments
            //
            // object Dummy { // scalafix:ok EscapeHatchDummyLinter
            //   1
            // }
            case FilterExpression(rules) => {
              if (!visitedFilterExpression.contains(comment.pos)) {
                // we approximate the position of the expression to the whole line
                val position = Position.Range(
                  comment.pos.input,
                  comment.pos.start - comment.pos.startColumn,
                  comment.pos.end
                )
                disable(EscapeOffset(position.start), comment, rules)
                enable(EscapeOffset(position.end), comment, rules)
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
