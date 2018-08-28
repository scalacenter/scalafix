package scalafix.internal.rule

import java.util.regex.Pattern
import metaconfig.Configured
import scala.collection.mutable
import scala.meta._
import scalafix.v1._

class RemoveUnused(config: RemoveUnusedConfig)
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
  // default constructor for reflective classloading
  def this() = this(RemoveUnusedConfig.default)

  override def description: String =
    "Rewrite that removes imports and terms that are reported as unused by the compiler under -Ywarn-unused"

  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (!config.scalacOptions.exists(_.startsWith("-Ywarn-unused"))) {
      Configured.error(
        s"The compiler option -Ywarn-unused is required to use RemoveUnused. " +
          "Run `scalac -Ywarn-unused:help` for more details. " +
          s"Obtained ${config.scalacOptions}")
    } else {
      config.conf
        .getOrElse("RemoveUnused")(this.config)
        .map(new RemoveUnused(_))
    }

  override def fix(implicit doc: SemanticDoc): Patch = {
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
      } else {
        () // ignore
      }
    }

    if (isUnusedImport.isEmpty && isUnusedTerm.isEmpty) {
      // Optimization: don't traverse if there are no diagnostics to act on.
      Patch.empty
    } else {
      doc.tree.collect {
        case i: Importee if isUnusedImport(importPosition(i)) =>
          i match {
            case Importee.Rename(_, to) =>
              // Unimport the identifier instead of removing the importee since
              // unused renamed may still impact compilation by shadowing an identifier.
              // See https://github.com/scalacenter/scalafix/issues/614
              Patch.replaceTree(to, "_").atomic
            case _ =>
              Patch.removeImportee(i).atomic
          }
        case i: Defn if isUnusedTerm(i.pos) =>
          defnTokensToRemove(i).map(Patch.removeTokens).asPatch.atomic
      }.asPatch
    }
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
