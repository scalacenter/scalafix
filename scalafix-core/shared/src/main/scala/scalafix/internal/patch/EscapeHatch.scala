package scalafix
package internal.patch

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.contrib._
import scala.meta.tokens.Token
import scalafix.internal.config.FilterMatcher
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch._
import scalafix.lint.LintMessage
import scalafix.patch._
import scalafix.rule.RuleName
import scalafix.util.TreeExtractors.Mods

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

    def isDisabledByEscape(name: RuleName, start: Int): Boolean =
      // annotatedEscapes takes precedence over anchoredEscapes
      annotatedEscapes.isEnabled(name, start) match {
        case (false, Some(culprit)) => // if disabled, there must be a escape
          usedEscapes += culprit
          true
        case _ =>
          val (enabled, culprit) = anchoredEscapes.isEnabled(name, start)
          culprit.foreach(escape => usedEscapes += escape)
          !enabled
      }

    def loop(name: RuleName, patch: Patch): Patch = patch match {
      case AtomicPatch(underlying) =>
        val hasDisabledPatch = {
          val patches = Patch.treePatchApply(underlying)(ctx, index)
          patches.exists { tp =>
            val byGit = diff.isDisabled(tp.tok.pos)
            val byEscape = isDisabledByEscape(name, tp.tok.pos.start)
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
        val byEscape = isDisabledByEscape(lint.id, lint.position.start)

        val isLintDisabled = byGit || byEscape

        if (!isLintDisabled) {
          lintMessages += lint
        }

        EmptyPatch

      case e => e
    }

    val patches =
      patchesByName.map { case (name, patch) => loop(name, patch) }.asPatch

    // we don't want to show unused warnings for non-Scalafix rules
    val prefixedEscapes =
      annotatedEscapes.disableEscapes
        .filter { escape =>
          val ruleName = escape.cause.text
          ruleName.startsWith(AnnotatedEscapes.OptionalRulePrefix)
        }
    val unusedDisable =
      (prefixedEscapes ++ anchoredEscapes.disableEscapes)
        .filterNot(usedEscapes)
        .map(_.cause)

    val unusedWarnings =
      (unusedDisable ++ anchoredEscapes.unusedEnable)
        .map(UnusedWarning.at)
    val warnings = lintMessages.result() ++ unusedWarnings
    (patches, warnings)
  }
}

object EscapeHatch {

  private val UnusedWarning = LintCategory
    .warning("", "Unused Scalafix suppression. This can be removed")
    .withOwner(RuleName("UnusedScalafixSuppression"))

  private type EscapeTree = TreeMap[EscapeOffset, List[EscapeFilter]]

  private case class EscapeFilter(
      matcher: FilterMatcher,
      cause: Position,
      startOffset: EscapeOffset,
      endOffset: Option[EscapeOffset] = None) {
    def matches(id: RuleName): Boolean =
      id.identifiers.exists(i => matcher.matches(i.value))
  }

  private case class EscapeOffset(offset: Int)

  private object EscapeOffset {
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
  private class AnnotatedEscapes private (escapeTree: EscapeTree) {

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

    def disableEscapes: Iterable[EscapeFilter] = escapeTree.values.flatten
  }

  private object AnnotatedEscapes {
    private val SuppressWarnings = "SuppressWarnings"
    private val SuppressAll = "all"
    val OptionalRulePrefix = "scalafix:"

    def apply(tree: Tree): AnnotatedEscapes = {
      val builder = TreeMap.newBuilder[EscapeOffset, List[EscapeFilter]]

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

      tree.foreach {
        case t @ Mods(mods) if hasSuppressWarnings(mods) =>
          addAnnotatedEscape(t, mods)
        case _ => ()
      }

      new AnnotatedEscapes(builder.result())
    }

    private def hasSuppressWarnings(mods: List[Mod]): Boolean =
      mods.exists {
        case Mod.Annot(Init(Type.Name(SuppressWarnings), _, _)) => true
        case Mod.Annot(
            Init(Type.Select(_, Type.Name(SuppressWarnings)), _, _)) =>
          true
        case _ => false
      }

    private def extractRules(mods: List[Mod]): List[(String, Position)] = {
      def process(rules: List[Term]) = rules.flatMap {
        case lit @ Lit.String(rule) =>
          val lo = lit.pos.start + 1 // drop leading quote
          val hi = lit.pos.end - 1 // drop trailing quote
          Some(rule -> Position.Range(lit.pos.input, lo, hi))

        case _ => None
      }

      mods.flatMap {
        case Mod.Annot(
            Init(
              Type.Name(SuppressWarnings),
              _,
              List(Term.Apply(Term.Name("Array"), rules) :: Nil))) =>
          process(rules)

        case Mod.Annot(
            Init(
              Type.Select(_, Type.Name(SuppressWarnings)),
              _,
              List(Term.Apply(Term.Name("Array"), rules) :: Nil))) =>
          process(rules)

        case _ => Nil
      }
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
  private class AnchoredEscapes private (
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

  private object AnchoredEscapes {
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
        rules.trim.split("\\s*,\\s*").toList

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
          if (currentlyDisabledRules.isEmpty && rulesToPos.isEmpty) { // wildcard
            unusedAnchoredEnable += anchor.pos
          } else {
            val disabledRules = currentlyDisabledRules.keySet
            rulesToPos.foreach {
              case (rule, pos) if !disabledRules(rule) =>
                unusedAnchoredEnable += pos
              case _ => ()
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
        case comment @ Token.Comment(rawComment) =>
          rawComment match {
            // matches off anchors
            //
            // // scalafix:off RuleA
            // ...
            //
            case FilterDisable(rules) =>
              disable(EscapeOffset(comment.pos.start), comment, rules)
              trackUnusedRules(comment, rules, enabled = false)

            // matches on anchors
            //
            // ...
            // // scalafix:on RuleA
            //
            case FilterEnable(rules) =>
              enable(EscapeOffset(comment.pos.start), comment, rules)
              trackUnusedRules(comment, rules, enabled = true)

            // matches expressions not handled by AssociatedComments
            //
            // object Dummy { // scalafix:ok NoDummy
            //   1
            // }
            case FilterExpression(rules) =>
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

            case _ => ()
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
