package scalafix.internal.rule

import scala.collection.mutable

import scala.meta._

import metaconfig.Configured
import scalafix.util.Trivia
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
  private def warnUnusedString = List("-Xlint", "-Xlint:unused")
  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val diagnosticsAvailableInSemanticdb =
      Seq("3.0", "3.1", "3.2", "3.3")
        .forall(v => !config.scalaVersion.startsWith(v))

    val hasWarnUnused = config.scalacOptions.exists(option =>
      warnUnusedPrefix.exists(prefix => option.startsWith(prefix)) ||
        warnUnusedString.contains(option)
    )
    if (!hasWarnUnused) {
      Configured.error(
        """|A Scala compiler option is required to use RemoveUnused. To fix this problem,
          |update your build to add -Ywarn-unused (with 2.12), -Wunused (with 2.13), or 
          |-Wunused:all (with 3.4+)""".stripMargin
      )
    } else if (!diagnosticsAvailableInSemanticdb) {
      Configured.error(
        "You must use a more recent version of the Scala 3 compiler (3.4+)"
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
          unusedPatterExpr.findFirstMatchIn(diagnostic.message).isDefined ||
            msg == "unused pattern variable"
        )
      ) {
        isUnusedPattern += diagnostic.position
      } else if (
        config.params &&
        (msg.startsWith("parameter") && msg.endsWith("is never used"))
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
            Patch.removeTokens(leftTokens(b, rhs))
        }
        cases
          .map { case Case(extract, _, _) => extract }
          .flatMap(_.collect(patchPatTree))
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

      doc.tree.collect {
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
          defnTokensToRemove(i).map(Patch.removeTokens).asPatch.atomic
        case i @ Defn.Def(_, name, _, _, _, _) if isUnusedTerm(name.pos) =>
          defnTokensToRemove(i).map(Patch.removeTokens).asPatch.atomic
        case i @ Defn.Val(_, List(pat), _, _)
            if isUnusedTerm.exists(p => p.start == pat.pos.start) =>
          defnTokensToRemove(i).map(Patch.removeTokens).asPatch.atomic
        case i @ Defn.Var(_, List(pat), _, _)
            if isUnusedTerm.exists(p => p.start == pat.pos.start) =>
          defnTokensToRemove(i).map(Patch.removeTokens).asPatch.atomic
        case Term.Match(_, cases) =>
          patchPatVarsIn(cases)
        case Term.PartialFunction(cases) =>
          patchPatVarsIn(cases)
        case Term.Try(_, cases, _) =>
          patchPatVarsIn(cases)
        case Term.Function(List(param @ Term.Param(_, name, None, _)), _)
            if isUnusedParam(
              // diagnostic does not include the implicit prefix (supported only
              // on 1-arity function without explicit type) captured in param,
              // so use name instead
              name.pos
            ) =>
          // drop the entire param so that the implicit prefix (if it exists) is removed
          Patch.replaceTree(param, "_")
        case Term.Function(List(param @ Term.Param(_, name, Some(_), _)), _)
            if isUnusedParam(
              // diagnostic does not include the wrapping parens captured in single param
              posExclParens(param)
            ) =>
          Patch.replaceTree(name, "_")
        case Term.Function(params, _) =>
          params.collect {
            case param @ Term.Param(_, name, _, _)
                if isUnusedParam(param.pos) =>
              Patch.replaceTree(name, "_")
          }.asPatch
      }.asPatch
    }
  }

  // Given ("val x = 2", "2"), returns "val x = ".
  private def leftTokens(t: Tree, right: Tree): Tokens =
    t.tokens.dropRightWhile(_.start >= right.pos.start)

  private def defnTokensToRemove(defn: Defn): Option[Tokens] = defn match {
    case i @ Defn.Val(_, _, _, Lit(_)) => Some(i.tokens)
    case i @ Defn.Val(_, _, _, rhs) => Some(leftTokens(i, rhs))
    case i @ Defn.Var(_, _, _, Some(Lit(_))) => Some(i.tokens)
    case i @ Defn.Var(_, _, _, rhs) => rhs.map(leftTokens(i, _))
    case i: Defn.Def => Some(i.tokens)
    case _ => None
  }

  private def posExclParens(tree: Tree): Position = {
    val leftTokenCount =
      tree.tokens.takeWhile(tk => tk.is[Token.LeftParen] || tk.is[Trivia]).size
    val rightTokenCount = tree.tokens
      .takeRightWhile(tk => tk.is[Token.RightParen] || tk.is[Trivia])
      .size
    tree.pos match {
      case Position.Range(input, start, end) =>
        Position.Range(input, start + leftTokenCount, end - rightTokenCount)
      case other => other
    }
  }
}
