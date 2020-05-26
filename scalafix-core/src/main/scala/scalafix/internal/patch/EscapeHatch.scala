package scalafix.internal.patch

import scala.annotation.tailrec
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.contrib._
import scala.meta.tokens.Token
import scala.meta.tokens.Token.Comment
import scalafix.internal.config.FilterMatcher
import scalafix.internal.diff.DiffDisable
import scalafix.internal.patch.EscapeHatch._
import scalafix.internal.util.LintSyntax._
import scalafix.internal.v1.LazyValue
import scalafix.lint.RuleDiagnostic
import scalafix.patch.Patch.internal._
import scalafix.rule.RuleName
import scalafix.util.TreeExtractors.Mods
import scalafix.v0._
import scala.collection.compat._ // used for cross-compilation.

/** EscapeHatch is an algorithm to selectively disable rules. There
 * are two mechanisms to do so: anchored comments and the
 * standard `@SuppressWarnings` annotation. The latter takes
 * precedence over the former in case there are overlaps.
 * See `AnchoredEscapes` and `AnnotatedEscapes` for more details.
 */
class EscapeHatch private (
    anchoredEscapes: AnchoredEscapes,
    annotatedEscapes: AnnotatedEscapes,
    diffDisable: DiffDisable
) {

  private def rawFilter(
      patchesByName: Map[RuleName, Patch]
  )(implicit ctx: RuleCtx): (Patch, List[RuleDiagnostic]) = {
    var patchBuilder = Patch.empty
    val diagnostics = List.newBuilder[RuleDiagnostic]
    patchesByName.foreach {
      case (rule, rulePatch) =>
        PatchInternals.foreach(rulePatch) {
          case LintPatch(message) =>
            diagnostics += message.toDiagnostic(rule, ctx.config)
          case rewritePatch =>
            patchBuilder += rewritePatch
        }
    }
    (patchBuilder, diagnostics.result())
  }

  def filter(patchesByName: Map[RuleName, Patch])(
      implicit ctx: RuleCtx,
      index: SemanticdbIndex
  ): (Patch, List[RuleDiagnostic]) = {
    if (isEmpty) return rawFilter(patchesByName)

    val usedEscapes = mutable.Set.empty[EscapeFilter]
    val lintMessages = List.newBuilder[RuleDiagnostic]

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
        val hasDisabledPatch =
          PatchInternals.treePatchApply(underlying).exists { tp =>
            val byGit = diffDisable.isDisabled(tp.tok.pos)
            val byEscape = isDisabledByEscape(name, tp.tok.pos.start)
            byGit || byEscape
          }
        if (hasDisabledPatch) EmptyPatch else underlying

      case Concat(a, b) => Concat(loop(name, a), loop(name, b))

      case LintPatch(lint) =>
        val byGit = diffDisable.isDisabled(lint.position)
        val id = lint.fullStringID(name)
        val byEscape = isDisabledByEscape(id, lint.position.start)
        val isLintDisabled = byGit || byEscape

        if (!isLintDisabled) {
          lintMessages += lint.toDiagnostic(name, ctx.config)
        }

        EmptyPatch

      case e => e
    }

    val patches = patchesByName.map {
      case (name, patch) => loop(name, patch)
    }.asPatch
    val unusedWarnings =
      (annotatedEscapes.unusedEscapes(usedEscapes) ++
        anchoredEscapes.unusedEscapes(usedEscapes)).map { pos =>
        UnusedScalafixSuppression.at(pos).toDiagnostic(UnusedName, ctx.config)
      }
    val warnings = lintMessages.result() ++ unusedWarnings
    (patches, warnings)
  }

  def isEmpty: Boolean =
    diffDisable.isEmpty && anchoredEscapes.isEmpty && annotatedEscapes.isEmpty
}

object EscapeHatch {
  private val UnusedScalafixSuppression =
    LintCategory.warning("", "Unused Scalafix suppression, this can be removed")
  private val UnusedName = RuleName("UnusedScalafixSuppression")

  private type EscapeTree = TreeMap[EscapeOffset, List[EscapeFilter]]
  private val EmptyEscapeTree: EscapeTree = TreeMap.empty

  private case class EscapeFilter(
      matcher: FilterMatcher,
      cause: Position,
      startOffset: EscapeOffset,
      endOffset: Option[EscapeOffset]
  ) {
    def matches(id: RuleName): Boolean =
      id.identifiers.exists(i => matcher.matches(i.value))
  }

  private case class EscapeOffset(offset: Int) extends AnyVal

  private object EscapeOffset {
    implicit val ordering: Ordering[EscapeOffset] = Ordering.by(_.offset)
  }

  def apply(
      input: Input,
      tree: LazyValue[Tree],
      associatedComments: LazyValue[AssociatedComments],
      diffDisable: DiffDisable
  ): EscapeHatch = {
    new EscapeHatch(
      AnchoredEscapes(input, tree, associatedComments),
      AnnotatedEscapes(input, tree),
      diffDisable
    )
  }

  /** Rules are disabled via standard `@SuppressWarnings` annotations. The
   * annotation can be placed in class, object, trait, type, def, val, var,
   * parameters and constructor definitions.
   *
   * Rules can be optionally prefixed with `scalafix:`. Besides helping
   * users to understand where the rules are coming from, it also allows
   * Scalafix to warn unused suppression.
   *
   * Use the keyword "all" to suppress all rules.
   */
  private class AnnotatedEscapes private (escapeTree: EscapeTree) {
    import AnnotatedEscapes._

    val isEmpty: Boolean = escapeTree.isEmpty

    def isEnabled(
        ruleName: RuleName,
        position: Int
    ): (Boolean, Option[EscapeFilter]) = {
      if (isEmpty) (true, None)
      else {
        val escapesUpToPos =
          escapeTree.rangeTo(EscapeOffset(position)).valuesIterator.flatten
        escapesUpToPos
          .collectFirst {
            case f @ EscapeFilter(_, _, _, Some(end))
                if end.offset >= position && f.matches(ruleName) =>
              (false, Some(f))
          }
          .getOrElse(true -> None)
      }
    }

    def unusedEscapes(used: collection.Set[EscapeFilter]): Set[Position] =
      escapeTree.valuesIterator.flatten.collect {
        case f @ EscapeFilter(_, rulePos, _, _)
            if !used(f) && rulePos.text.startsWith(OptionalRulePrefix) => // only report unused warnings for Scalafix rules
          rulePos
      }.toSet
  }

  private object AnnotatedEscapes {
    private val SuppressWarnings = "SuppressWarnings"
    private val SuppressAll = "all"
    private val OptionalRulePrefix = "scalafix:"

    def apply(input: Input, tree: LazyValue[Tree]): AnnotatedEscapes = {
      if (input.text.contains(SuppressWarnings)) collectEscapes(tree.value)
      else new AnnotatedEscapes(EmptyEscapeTree)
    }

    private def collectEscapes(tree: Tree): AnnotatedEscapes = {
      val escapes =
        tree.collect {
          case t @ Mods(SuppressWarningsArgs(args)) =>
            val start = EscapeOffset(t.pos.start)
            val end = EscapeOffset(t.pos.end)
            val rules = rulesWithPosition(args)
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

            (start, filters.result())
        }
      new AnnotatedEscapes(TreeMap(escapes: _*))
    }

    private object SuppressWarningsArgs {
      def unapply(mods: List[Mod]): Option[List[Term]] =
        mods.collectFirst {
          case Mod.Annot(
              Init(
                Type.Name(SuppressWarnings),
                _,
                List(Term.Apply(Term.Name("Array"), args) :: Nil)
              )
              ) =>
            args

          case Mod.Annot(
              Init(
                Type.Select(_, Type.Name(SuppressWarnings)),
                _,
                List(Term.Apply(Term.Name("Array"), args) :: Nil)
              )
              ) =>
            args
        }
    }

    private def rulesWithPosition(rules: List[Term]): List[(String, Position)] =
      rules.collect {
        case lit @ Lit.String(rule) =>
          // get the exact position of the rule name
          val lo = lit.pos.start + lit.pos.text.indexOf(rule)
          val hi = lo + rule.length
          rule -> Position.Range(lit.pos.input, lo, hi)
      }
  }

  /**
   * Rules are disabled via comments with a specific syntax:
   * `scalafix:off` or `scalafix:on` disable or enable rules until the end of file
   * `scalafix:ok` disable rules on an associated expression
   * a list of rules separated by commas can be provided to selectively
   * enable or disable rules otherwise all rules are affected
   *
   * `enabling` and `enabling` contain the offset at which you start applying a filter
   *
   * `unused` contains the position of unused `scalafix:on|off`
   */
  private class AnchoredEscapes private (
      enabling: EscapeTree,
      disabling: EscapeTree,
      unused: List[Position]
  ) {

    val isEmpty: Boolean = disabling.isEmpty && unused.isEmpty

    /**
     * a rule r is disabled in position p if there is a comment disabling r at
     * position p1 < p and there is no comment enabling r in position p2 where p1 < p2 < p.
     */
    def isEnabled(
        ruleName: RuleName,
        position: Int
    ): (Boolean, Option[EscapeFilter]) = {
      def findEnableInRange(from: EscapeOffset, to: EscapeOffset) = {
        val enables = enabling.range(from, to).valuesIterator.flatten
        enables.find(_.matches(ruleName))
      }

      @tailrec
      def loop(
          disables: List[EscapeFilter],
          culprit: Option[EscapeFilter]
      ): (Boolean, Option[EscapeFilter]) = {
        disables match {
          case head :: tail if head.matches(ruleName) =>
            head.endOffset match {
              case Some(end) =>
                if (position < end.offset) (false, Some(head))
                else loop(tail, culprit)
              case None => // EOF
                val maybeEnable =
                  findEnableInRange(head.startOffset, EscapeOffset(position))
                if (!maybeEnable.isDefined) (false, Some(head))
                else loop(tail, maybeEnable)
            }
          case _ :: tail => loop(tail, culprit)
          case Nil => (true, culprit)
        }
      }

      if (disabling.isEmpty) (true, None)
      else {
        val disables =
          disabling.rangeTo(EscapeOffset(position)).valuesIterator.flatten
        loop(disables.toList, None)
      }
    }

    def unusedEscapes(used: collection.Set[EscapeFilter]): Set[Position] =
      unused.toSet ++
        disabling.valuesIterator.flatten.filterNot(used).map(_.cause)
  }

  private object AnchoredEscapes {
    private val Prefix = "scalafix:"
    private val ScalafixOk = Prefix + "ok"
    private val ScalafixOn = Prefix + "on"
    private val ScalafixOff = Prefix + "off"
    private val ScalafixOkRgx = regex(ScalafixOk)
    private val ScalafixOnRgx = regex(ScalafixOn)
    private val ScalafixOffRgx = regex(ScalafixOff)

    private def regex(anchor: String) = s"\\s?$anchor\\s?(.*?)(?:;.*)?".r

    def apply(
        input: Input,
        tree: LazyValue[Tree],
        associatedComments: LazyValue[AssociatedComments]
    ): AnchoredEscapes = {
      if (input.text.contains(Prefix))
        collectEscapes(input, tree.value, associatedComments)
      else new AnchoredEscapes(EmptyEscapeTree, EmptyEscapeTree, Nil)
    }

    private def collectEscapes(
        input: Input,
        tree: Tree,
        associatedComments: LazyValue[AssociatedComments]
    ): AnchoredEscapes = {
      val enable = TreeMap.newBuilder[EscapeOffset, List[EscapeFilter]]
      val disable = TreeMap.newBuilder[EscapeOffset, List[EscapeFilter]]
      val visitedFilterExpression = mutable.Set.empty[Position]
      val onOffTracker = new OnOffTracker

      def makeFilters(
          rulesPos: List[(String, Position)],
          start: EscapeOffset,
          end: Option[EscapeOffset],
          anchor: Comment
      ): List[EscapeFilter] = {
        if (rulesPos.isEmpty) { // wildcard
          List(
            EscapeFilter(FilterMatcher.matchEverything, anchor.pos, start, end)
          )
        } else {
          rulesPos.map {
            case (rule, pos) =>
              EscapeFilter(FilterMatcher(rule), pos, start, end)
          }
        }
      }

      def splitRules(rules: String): List[String] = {
        val trimmed = rules.trim
        if (trimmed.isEmpty) Nil else trimmed.split("\\s*,\\s*").toList
      }

      def rulesWithPosition(
          rules: String,
          anchor: Comment
      ): List[(String, Position)] = {
        val rulesToPos = ListBuffer.empty[(String, Position)]
        var fromIdx = 0

        for (rule <- splitRules(rules)) {
          val idx = anchor.text.indexOf(rule, fromIdx)
          val startPos = anchor.start + idx
          val endPos = startPos + rule.length
          val pos = Position.Range(anchor.input, startPos, endPos)
          fromIdx = idx + rule.length
          rulesToPos += (rule -> pos)
        }
        rulesToPos.result()
      }

      if (input.text.contains(ScalafixOk)) {
        tree.foreach { t =>
          associatedComments.value.trailing(t).foreach {
            // matches simple expressions
            //
            // val a = (
            //   1,
            //   2
            // ) // scalafix:ok RuleA
            //
            case comment @ Token.Comment(ScalafixOkRgx(rules)) =>
              if (!visitedFilterExpression.contains(comment.pos)) {
                val rulesPos = rulesWithPosition(rules, comment)
                val start = EscapeOffset(t.pos.start)
                val end = Some(EscapeOffset(t.pos.end))
                disable += (start -> makeFilters(rulesPos, start, end, comment))
                visitedFilterExpression += comment.pos
              }

            case _ => ()
          }
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
            case ScalafixOffRgx(rules) =>
              val rulesPos = rulesWithPosition(rules, comment)
              val start = EscapeOffset(comment.pos.start)
              disable += (start -> makeFilters(rulesPos, start, None, comment))
              onOffTracker.trackOff(rulesPos, comment)

            // matches on anchors
            //
            // ...
            // // scalafix:on RuleA
            //
            case ScalafixOnRgx(rules) =>
              val rulesPos = rulesWithPosition(rules, comment)
              val start = EscapeOffset(comment.pos.start)
              enable += (start -> makeFilters(rulesPos, start, None, comment))
              onOffTracker.trackOn(rulesPos, comment)

            // matches expressions not handled by AssociatedComments
            //
            // object Dummy { // scalafix:ok NoDummy
            //   1
            // }
            case ScalafixOkRgx(rules) =>
              if (!visitedFilterExpression.contains(comment.pos)) {
                val rulesPos = rulesWithPosition(rules, comment)
                // we approximate the position of the expression to the whole line
                val start =
                  EscapeOffset(comment.pos.start - comment.pos.startColumn)
                val end = Some(EscapeOffset(comment.pos.end))
                disable += (start -> makeFilters(rulesPos, start, end, comment))
              }

            case _ => ()
          }

        case _ => ()
      }

      new AnchoredEscapes(
        enable.result(),
        disable.result(),
        onOffTracker.allUnused
      )
    }
  }

  /**
   * Keep track of what rules are ON or OFF while the `scalafix:on|off` comments
   * are being parsed from the top to the bottom of the source file. This makes
   * it possible to identify unused escape comments and duplicate rules.
   *
   * This code does not know upfront what rules are configured for a given file.
   * Therefore, it uses a placeholder to represent "all".
   */
  private class OnOffTracker {
    private trait Selection
    private case object AllRules extends Selection
    private case class SomeRules(names: Set[String] = Set()) extends Selection

    private val unused = List.newBuilder[Position]
    private var currentlyOn: Selection = AllRules
    private var currentlyOff: Selection = SomeRules()

    def trackOn(rulesPos: List[(String, Position)], anchor: Comment): Unit = {
      val (off, on) = update(rulesPos, anchor, currentlyOff, currentlyOn)
      currentlyOff = off
      currentlyOn = on
    }

    def trackOff(rulesPos: List[(String, Position)], anchor: Comment): Unit = {
      val (on, off) = update(rulesPos, anchor, currentlyOn, currentlyOff)
      currentlyOn = on
      currentlyOff = off
    }

    private def update(
        rulesPos: List[(String, Position)],
        anchor: Comment,
        source: Selection,
        target: Selection
    ): (Selection, Selection) =
      rulesPos match {
        case Nil => // wildcard
          (source, target) match {
            case (SomeRules(src), AllRules) if src.isEmpty =>
              unused += anchor.pos // everything is already in the target state
            case _ =>
          }
          (SomeRules(), AllRules)

        case _ => // specific rules
          ((source, target): @unchecked) match {
            case (AllRules, SomeRules(tgt)) =>
              var newTgt = tgt
              rulesPos.foreach {
                case (rule, pos) =>
                  if (!newTgt(rule)) newTgt += rule else unused += pos
              }
              (AllRules, SomeRules(newTgt))

            case (SomeRules(src), AllRules) =>
              var newSrc = src
              rulesPos.foreach {
                case (rule, pos) =>
                  if (newSrc(rule)) newSrc -= rule else unused += pos
              }
              (SomeRules(newSrc), AllRules)
          }
      }

    def allUnused: List[Position] = unused.result()
  }
}
