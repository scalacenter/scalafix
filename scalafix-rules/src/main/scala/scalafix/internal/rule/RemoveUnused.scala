package scalafix.internal.rule

import java.util.regex.Pattern
import metaconfig.Configured
import scala.collection.mutable
import scala.meta._
import scalafix.v1._

class RemoveUnused
    extends SemanticRule(
      RuleName("RemoveUnused")
        .withDeprecatedName(
          "RemoveUnusedImports",
          "Use RemoveUnused instead",
          "0.6.0")
        .withDeprecatedName(
          "RemoveUnusedTerms",
          "Use RemoveUnused instead",
          "0.6.0")
    ) {

  override def description: String =
    "Rewrite that removes imports and terms that are reported as unused by the compiler under -Ywarn-unused"

  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (!config.scalacOptions.exists(_.startsWith("-Ywarn-unused"))) {
      Configured.error(
        s"The compiler option -Ywarn-unused is required to use RemoveUnused. " +
          "Run `scalac -Ywarn-unused:help` for more details. " +
          s"Obtained ${config.scalacOptions}")
    } else {
      Configured.ok(this)
    }

  override def fix(implicit doc: SemanticDoc): Patch = {
    val isUnusedTerm = mutable.Set.empty[Position]
    val isUnusedImport = mutable.Set.empty[Position]

    doc.diagnostics.foreach {
      case message if message.message == "Unused import" =>
        isUnusedImport += message.position
      case message if isUnusedPrivateDiagnostic(message) =>
        isUnusedTerm += message.position
      case _ =>
    }

    doc.tree.collect {
      case i: Importee if isUnusedImport(importPosition(i)) =>
        Patch.removeImportee(i).atomic
      case i: Defn if isUnusedTerm(i.pos) =>
        defnTokensToRemove(i).map(Patch.removeTokens).asPatch.atomic
    }.asPatch
  }

  private val unusedPrivateLocalVal: Pattern =
    Pattern.compile("""(local|private) (.*) is never used""")
  def isUnusedPrivateDiagnostic(message: Diagnostic): Boolean =
    unusedPrivateLocalVal.matcher(message.message).matches()

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
