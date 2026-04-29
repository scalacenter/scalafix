package scalafix.internal.rule

import scala.collection.mutable

import scala.meta._
import scala.meta.tokens.Token

import metaconfig.Configured
import scalafix.util.TreeOps
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
      Seq("3.0", "3.1", "3.2", "3.3.0", "3.3.1", "3.3.2", "3.3.3")
        .forall(v => !config.scalaVersion.startsWith(v))

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
    val isUnusedTerm = mutable.Set.empty[Position]
    val isUnusedImport = mutable.Set.empty[Position]
    val isUnusedPattern = mutable.Set.empty[Position]
    val isUnusedParam = mutable.Set.empty[Position]

    val unusedPatterExpr =
      raw"^pattern var .* in (value|method) .* is never used".r

    doc.diagnostics.foreach { diagnostic =>
      val msg = diagnostic.message
      if (config.imports && diagnostic.message.toLowerCase == "unused import") {
        isUnusedImport += diagnostic.position
      } else if (
        config.privates &&
        (
          (msg.startsWith("private") && msg.endsWith("is never used")) ||
            msg == "unused private member"
        )
      ) {
        isUnusedTerm += diagnostic.position
      } else if (
        config.locals &&
        (
          (msg.startsWith("local") && msg.endsWith("is never used")) ||
            msg == "unused local definition"
        )
      ) {
        isUnusedTerm += diagnostic.position
      } else if (
        config.patternvars &&
        (
          unusedPatterExpr.findFirstMatchIn(msg).isDefined ||
            msg == "unused pattern variable"
        )
      ) {
        isUnusedPattern += diagnostic.position
      } else if (
        config.params &&
        (
          msg.startsWith("parameter") && msg.endsWith("is never used") ||
            msg == "unused explicit parameter"
        )
      ) {
        isUnusedParam += diagnostic.position
      }
    }

    if (
      isUnusedImport.isEmpty && isUnusedTerm.isEmpty && isUnusedPattern.isEmpty && isUnusedParam.isEmpty
    ) {
      // Optimization: don't traverse if there are no diagnostics to act on.
      Patch.empty
    } else {
      def patchPatVarsIn(cases: List[Case]): Patch = {
        def checkUnusedPatTree(t: Tree): Boolean =
          isUnusedPattern(t.pos) || isUnusedPattern(posExclParens(t))
        def patchPatTree: PartialFunction[Tree, Patch] = {
          case v: Pat.Var if checkUnusedPatTree(v) =>
            Patch.replaceTree(v, "_")
          case t @ Pat.Typed(v: Pat.Var, _) if checkUnusedPatTree(t) =>
            Patch.replaceTree(v, "_")
          case b @ Pat.Bind(_, rhs) if checkUnusedPatTree(b) =>
            val (heading, trailing) = surroundingTokens(b, rhs)
            Patch.removeTokens(heading ++ trailing)
        }
        cases
          .map { case Case(extract, _, _) => extract }
          .flatMap(TreeOps.collectTree(patchPatTree))
          .asPatch
          .atomic
      }

      def isUnusedImportee(importee: Importee): Boolean = {
        val pos = importee match {
          case Importee.Rename(from, _) => from.pos
          case _ => importee.pos
        }
        isUnusedImport.exists { unused =>
          unused.start <= pos.start && pos.end <= unused.end
        }
      }

      TreeOps.collectTree {
        case Importer(_, importees)
            if importees.forall(_.is[Importee.Unimport]) =>
          importees.map(Patch.removeImportee).asPatch
        case Importer(_, importees) =>
          val hasUsedWildcard = importees.exists {
            case i: Importee.Wildcard => !isUnusedImportee(i)
            case _ => false
          }
          importees.collect {
            case i @ Importee.Rename(_, to)
                if isUnusedImportee(i) && hasUsedWildcard =>
              // Unimport the identifier instead of removing the importee since
              // unused renamed may still impact compilation by shadowing an identifier.
              // See https://github.com/scalacenter/scalafix/issues/614
              Patch.replaceTree(to, "_").atomic
            case i if isUnusedImportee(i) =>
              Patch.removeImportee(i).atomic
          }.asPatch
        case i: Defn if isUnusedTerm(i.pos) =>
          defnPatch(i).asPatch.atomic
        case i: Defn.Def if isUnusedTerm(i.name.pos) =>
          defnPatch(i).asPatch.atomic
        case i @ Defn.Val(_, List(pat), _, _) if isUnusedTerm.exists { p =>
              p.start == pat.pos.start ||
              p.start == i.pos.start // scala 2.12
            } =>
          defnPatch(i).asPatch.atomic
        case i @ Defn.Var.Initial(_, List(pat), _, _) if isUnusedTerm.exists {
              p =>
                p.start == pat.pos.start ||
                p.start == i.pos.start // scala 2.12
            } =>
          defnPatch(i).asPatch.atomic
        case i: Term.Match =>
          patchPatVarsIn(i.casesBlock.cases)
        case Term.PartialFunction(cases) =>
          patchPatVarsIn(cases)
        case i: Term.Try =>
          patchPatVarsIn(i.cases)
        case Term.Function.Initial(
              List(param @ Term.Param(_, name, None, _)),
              _
            )
            if isUnusedParam(
              // diagnostic does not include the implicit prefix (supported only
              // on 1-arity function without explicit type) captured in param,
              // so use name instead
              name.pos
            ) =>
          // drop the entire param so that the implicit prefix (if it exists) is removed
          Patch.replaceTree(param, "_")
        case Term.Function.Initial(
              List(param @ Term.Param(_, name, Some(_), _)),
              _
            )
            if isUnusedParam(
              // diagnostic does not include the wrapping parens captured in single param
              posExclParens(param)
            ) =>
          Patch.replaceTree(name, "_")
        case t: Term.Function =>
          t.paramClause.values.collect {
            case param @ Term.Param(_, name, _, _)
                if isUnusedParam(param.pos) || isUnusedParam(name.pos) =>
              Patch.replaceTree(name, "_")
          }.asPatch
      }(doc.tree).asPatch
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

  private def posExclParens(tree: Tree): Position = {
    tree.pos match {
      case Position.Range(input, start, end) =>
        val tokens = tree.tokens
        val leftCount = tokens.skipIf(_.isAny[Token.LeftParen, Token.Trivia])
        val rightCount = tokens.length - 1 -
          tokens.rskipIf(_.isAny[Token.RightParen, Token.Trivia])
        Position.Range(input, start + leftCount, end - rightCount)
      case other => other
    }
  }
}
