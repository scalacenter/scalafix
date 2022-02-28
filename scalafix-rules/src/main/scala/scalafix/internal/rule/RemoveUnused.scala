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
    "Removes unused imports and terms that reported by the compiler under -Ywarn-unused"
  override def isRewrite: Boolean = true

  private def warnUnusedPrefix = List("-Wunused", "-Ywarn-unused")
  private def warnUnusedString = List("-Xlint", "-Xlint:unused")
  override def withConfiguration(config: Configuration): Configured[Rule] = {
    val hasWarnUnused = config.scalacOptions.exists(option =>
      warnUnusedPrefix.exists(prefix => option.startsWith(prefix)) ||
        warnUnusedString.contains(option)
    )
    if (config.scalaVersion.startsWith("3"))
      Configured.error(
        "This rule is specific to Scala 2, because the compiler option `-Ywarn-unused` is not available yet in scala 3 " +
          "To fix this error, remove RemoveUnused from .scalafix.conf"
      )
    else if (!hasWarnUnused) {
      Configured.error(
        s"""|The Scala compiler option "-Ywarn-unused" is required to use RemoveUnused.
          |To fix this problem, update your build to use at least one Scala compiler
          |option like -Ywarn-unused, -Xlint:unused (2.12.2 or above), or -Wunused (2.13 only)""".stripMargin
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
      if (config.imports && diagnostic.message == "Unused import") {
        isUnusedImport += diagnostic.position
      } else if (
        config.privates &&
        diagnostic.message.startsWith("private") &&
        diagnostic.message.endsWith("is never used")
      ) {
        isUnusedTerm += diagnostic.position
      } else if (
        config.locals &&
        diagnostic.message.startsWith("local") &&
        diagnostic.message.endsWith("is never used")
      ) {
        isUnusedTerm += diagnostic.position
      } else if (
        config.patternvars &&
        unusedPatterExpr.findFirstMatchIn(diagnostic.message).isDefined
      ) {
        isUnusedPattern += diagnostic.position
      } else if (
        config.params &&
        diagnostic.message.startsWith("parameter") &&
        diagnostic.message.endsWith("is never used")
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

      doc.tree.collect {
        case Importer(_, importees)
            if importees.forall(_.is[Importee.Unimport]) =>
          importees.map(Patch.removeImportee).asPatch
        case Importer(_, importees) =>
          val hasUsedWildcard = importees.exists {
            case i: Importee.Wildcard => !isUnusedImport(importPosition(i))
            case _ => false
          }
          importees.collect {
            case i @ Importee.Rename(_, to)
                if isUnusedImport(importPosition(i)) && hasUsedWildcard =>
              // Unimport the identifier instead of removing the importee since
              // unused renamed may still impact compilation by shadowing an identifier.
              // See https://github.com/scalacenter/scalafix/issues/614
              Patch.replaceTree(to, "_").atomic
            case i if isUnusedImport(importPosition(i)) =>
              Patch.removeImportee(i).atomic
          }.asPatch
        case i: Defn if isUnusedTerm(i.pos) =>
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

  private def importPosition(importee: Importee): Position = importee match {
    case Importee.Rename(from, _) => from.pos
    case _ => importee.pos
  }

  // Given ("val x = 2", "2"), returns "val x = ".
  private def leftTokens(t: Tree, right: Tree): Tokens = {
    val startT = t.tokens.start
    val startRight = right.tokens.start
    t.tokens.take(startRight - startT)
  }

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
