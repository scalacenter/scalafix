package scalafix.internal.rule

import scala.annotation.tailrec
import scala.collection.mutable

import scala.meta._
import scala.meta.tokens.Token

import metaconfig.Configured
import scalafix.internal.config.ScalaVersion
import scalafix.internal.util.ImporterOps._
import scalafix.v1._

class RemoveUnused(config: RemoveUnusedConfig)
    extends SemanticRule(
      RuleName("RemoveUnused")
        .withDeprecatedName(
          "RemoveUnusedImports",
          "Use RemoveUnused instead",
          "0.6.0"
        )
        .withDeprecatedName(
          "RemoveUnusedTerms",
          "Use RemoveUnused instead",
          "0.6.0"
        )
    ) {
  // default constructor for reflective classloading
  def this() = this(RemoveUnusedConfig.default)

  override def description: String =
    "Removes unused imports and terms that reported by the compiler under -Wunused"
  override def isRewrite: Boolean = true

  private def warnUnusedPrefix = List("-Wunused", "-Ywarn-unused")
  private def warnUnusedString = List("-Wall", "-Xlint", "-Xlint:unused")
  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val diagnosticsAvailableInSemanticdb =
      ScalaVersion.unusedDiagnosticsInSemanticdb(config.scalaVersion)

    val hasWarnUnused = config.scalacOptions.exists(option =>
      warnUnusedPrefix.exists(prefix => option.startsWith(prefix)) ||
        warnUnusedString.contains(option)
    )
    if (!hasWarnUnused) {
      Configured.error(
        """|A Scala compiler option is required to use RemoveUnused. To fix this problem,
          |update your build to add -Ywarn-unused (with 2.12), -Wunused (with 2.13),
          |-Wunused:all (with 3.3.4+), or -Wall (with 3.5.2+ and backported to 3.3.5+)""".stripMargin
      )
    } else if (!diagnosticsAvailableInSemanticdb) {
      Configured.error(
        "You must use a more recent version of the Scala 3 compiler (3.3.4+)"
      )
    } else {
      config.conf
        .getOrElse("RemoveUnused")(this.config)
        .map(new RemoveUnused(_))
    }
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    val isUnusedTerm = mutable.LongMap.empty[Unit] // starts
    val isUnusedImportee = new OrganizeImports.UnusedImporteePositions(
      if (config.imports) doc.diagnostics else Iterator.empty
    )
    val isUnusedPattern = mutable.LongMap.empty[Unit] // starts
    val isUnusedParam = mutable.LongMap.empty[Unit] // starts

    val unusedPatterExpr =
      raw"^pattern var .* in (value|method) .* is never used".r

    doc.diagnostics.foreach { diagnostic =>
      val msg = diagnostic.message
      if (
        config.privates &&
        (
          (msg.startsWith("private") && msg.endsWith("is never used")) ||
            msg == "unused private member"
        )
      ) {
        isUnusedTerm(diagnostic.position.start) = ()
      } else if (
        config.locals &&
        (
          (msg.startsWith("local") && msg.endsWith("is never used")) ||
            msg == "unused local definition"
        )
      ) {
        isUnusedTerm(diagnostic.position.start) = ()
      } else if (
        config.patternvars &&
        (
          unusedPatterExpr.findFirstMatchIn(msg).isDefined ||
            msg == "unused pattern variable"
        )
      ) {
        isUnusedPattern(diagnostic.position.start) = ()
      } else if (
        config.params &&
        (
          msg.startsWith("parameter") && msg.endsWith("is never used") ||
            msg == "unused explicit parameter"
        )
      ) {
        isUnusedParam(diagnostic.position.start) = ()
      }
    }

    if (
      isUnusedImportee.isEmpty && isUnusedTerm.isEmpty && isUnusedPattern.isEmpty && isUnusedParam.isEmpty
    ) {
      // Optimization: don't traverse if there are no diagnostics to act on.
      Patch.empty
    } else {
      // a diagnostic may also start at a paren that wraps only the variable
      def isUnusedPatVar(v: Pat.Var): Boolean =
        isUnusedPattern.contains(v.name.pos.start) || {
          val toks = v.name.tokens
          @tailrec
          def inParens(beg: Int, end: Int): Boolean = {
            val lp = toks.rskipWideIf(_.is[Token.Trivia], beg)
            val rp = toks.skipWideIf(_.is[Token.Trivia], end)
            toks.getWideOrNull(lp) match {
              case t: Token.LeftParen
                  if toks.getWideOrNull(rp).isInstanceOf[Token.RightParen] =>
                isUnusedPattern.contains(t.start) || inParens(lp - 1, rp + 1)
              case _ => false
            }
          }
          inParens(-1, toks.length)
        }

      val patchPatTree: PartialFunction[Tree, Patch] = {
        case v: Pat.Var if isUnusedPatVar(v) =>
          v.parent match {
            case Some(b: Pat.Bind) if b.lhs eq v =>
              val (heading, trailing) = surroundingTokens(b, b.rhs)
              Patch.removeTokens(heading ++ trailing)
            case _ => Patch.replaceTree(v.name, "_")
          }
      }

      val addParamPatch: (Patch, Term.Param) => Patch = (acc, param) =>
        if (isUnusedParam.contains(param.name.pos.start)) {
          // drop an untyped param whole, with its `implicit` prefix
          if (param.decltpe.isEmpty) acc + Patch.replaceTree(param, "_")
          else {
            val patchImplicit = // drop `implicit` too: Scala 2.12 does not parse `implicit _: T`
              param.mods.find(_.is[Mod.Implicit]) match {
                case Some(mod) =>
                  val tokens = mod.tokens
                  val len = tokens.length
                  val wsEnd = tokens.skipWideIf(_.is[Token.Whitespace], len)
                  val hasComment =
                    tokens.getWideOrNull(wsEnd).isInstanceOf[Token.HTrivia]
                  val end = if (hasComment) len else wsEnd
                  Patch.replaceToken(tokens.head, "(") + {
                    if (end > 1) Patch.removeTokens(tokens.sliceWide(1, end))
                    else Patch.empty
                  } + Patch.addRight(param, ")")
                case _ => Patch.empty
              }
            acc + Patch.replaceTree(param.name, "_") + patchImplicit
          }
        } else acc

      def patchPatVarsIn(cases: List[Case]): Patch =
        cases
          .map { case Case(extract, _, _) => extract }
          .flatMap(_.collect(patchPatTree))
          .asPatch
          .atomic

      def isUnusedDefn(defn: Tree, lhs: Tree): Boolean =
        isUnusedTerm.contains(lhs.pos.start) ||
          isUnusedTerm.contains(defn.pos.start)

      doc.tree.collect {
        case importer: Importer
            if importer.values.forall(_.is[Importee.Unimport]) =>
          importer.values.map(Patch.removeImportee).asPatch
        case importer: Importer =>
          val (unused, used) = importer.values.partition(isUnusedImportee)
          val hasUsedWildcard = used.exists(_.is[Importee.Wildcard])
          unused.map {
            case i: Importee.Rename if hasUsedWildcard =>
              // Unimport the identifier instead of removing the importee since
              // unused renamed may still impact compilation by shadowing an identifier.
              // See https://github.com/scalacenter/scalafix/issues/614
              Patch.replaceTree(i.rename, "_").atomic
            case i =>
              Patch.removeImportee(i).atomic
          }.asPatch
        case i: Defn.Def if isUnusedDefn(i, i.name) =>
          defnPatch(i).asPatch.atomic
        case i @ Defn.Val(_, List(pat), _, _) if isUnusedDefn(i, pat) =>
          defnPatch(i).asPatch.atomic
        case i @ Defn.Var.Initial(_, List(pat), _, _) if isUnusedDefn(i, pat) =>
          defnPatch(i).asPatch.atomic
        case i: Term.Match =>
          patchPatVarsIn(i.casesBlock.cases)
        case Term.PartialFunction(cases) =>
          patchPatVarsIn(cases)
        case i: Term.Try =>
          patchPatVarsIn(i.cases)
        case t: Term.Function =>
          t.paramClause.values.foldLeft(Patch.empty)(addParamPatch)
      }.asPatch
    }
  }

  // Returns the tokens of outer that surround inner: those before inner
  // (heading) and those after inner (trailing). Callers can remove both to
  // strip the definition while keeping inner and avoiding unbalanced parens.
  // Given ("val x = 2", "2"), returns ("val x = ", Seq.empty).
  // Given ("val x = (2 + 3)", "2 + 3"), returns ("val x = (", Seq(")")).
  private def surroundingTokens(outer: Tree, inner: Tree): (Tokens, Tokens) = {
    val heading = outer.tokens.takeWhile(_.start < inner.pos.start)
    val trailing = outer.tokens.takeRightWhile(_.start >= inner.pos.end)
    (heading, trailing)
  }

  private def defnTokensToRemove(defn: Defn): Option[(Tokens, Seq[Token])] =
    defn match {
      case i: Defn.Val =>
        i.rhs match {
          case _: Lit => Some((i.tokens, Seq.empty))
          case rhs => Some(surroundingTokens(i, rhs))
        }
      case i: Defn.Var =>
        i.body match {
          case _: Lit => Some((i.tokens, Seq.empty))
          case rhs => Some(surroundingTokens(i, rhs))
        }
      case i: Defn.Def => Some((i.tokens, Seq.empty))
      case _ => None
    }

  private def defnPatch(defn: Defn): Option[Patch] =
    defnTokensToRemove(defn).map { case (heading, trailing) =>
      val maybeRHSBlock = (defn match {
        case t: Defn.Val => Some(t.rhs)
        case t: Defn.Var => Some(t.body)
        case _ => None
      }).collect { case x: Term.Block => x }

      val maybeLocally = maybeRHSBlock.map { block =>
        if (block.stats.headOption.exists(_.pos.start == block.pos.start))
          // only significant indentation blocks have their first stat
          // aligned with the block itself (otherwise there is a heading "{")
          "locally:"
        else "locally"
      }

      val headingPatch = maybeLocally match {
        case Some(locally) =>
          // Preserve comments between the LHS and the RHS, as well as
          // newlines & whitespaces for significant indentation
          val tokensNoTrailingTrivia =
            heading.dropRightWhile(_.is[Token.Trivia])

          Patch.removeTokens(tokensNoTrailingTrivia) +
            tokensNoTrailingTrivia.lastOption.map(Patch.addRight(_, locally))
        case _ =>
          Patch.removeTokens(heading)
      }

      headingPatch + Patch.removeTokens(trailing)
    }
}
