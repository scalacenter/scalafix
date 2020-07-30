package scalafix.internal.rule

import scala.collection.mutable

import scala.meta._

import metaconfig.Configured
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
    if (!hasWarnUnused) {
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

    doc.diagnostics.foreach { diagnostic =>
      if (config.imports && diagnostic.message == "Unused import") {
        isUnusedImport += diagnostic.position
      } else if (config.privates &&
        diagnostic.message.startsWith("private") &&
        diagnostic.message.endsWith("is never used")) {
        isUnusedTerm += diagnostic.position
      } else if (config.locals &&
        diagnostic.message.startsWith("local") &&
        diagnostic.message.endsWith("is never used")) {
        isUnusedTerm += diagnostic.position
      }
    }

    if (isUnusedImport.isEmpty && isUnusedTerm.isEmpty) {
      // Optimization: don't traverse if there are no diagnostics to act on.
      Patch.empty
    } else {
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
      }.asPatch
    }
  }

  private def importPosition(importee: Importee): Position = importee match {
    case Importee.Rename(from, _) => from.pos
    case _ => importee.pos
  }

  // Given defn "val x = 2", returns "val x = ".
  private def binderTokens(defn: Defn, body: Term): Tokens = {
    val startDef = defn.tokens.start
    val startBody = body.tokens.start
    defn.tokens.take(startBody - startDef)
  }

  private def defnTokensToRemove(defn: Defn): Option[Tokens] = defn match {
    case i @ Defn.Val(_, _, _, Lit(_)) => Some(i.tokens)
    case i @ Defn.Val(_, _, _, rhs) => Some(binderTokens(i, rhs))
    case i @ Defn.Var(_, _, _, Some(Lit(_))) => Some(i.tokens)
    case i @ Defn.Var(_, _, _, rhs) => rhs.map(binderTokens(i, _))
    case i: Defn.Def => Some(i.tokens)
    case _ => None
  }
}
