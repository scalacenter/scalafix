package scalafix.internal.rule

import java.util.regex.Pattern
import scala.meta._
import scalafix.v1._

class RemoveUnusedTerms extends SemanticRule("RemoveUnusedTerms") {

  override def description: String =
    "Rewrite that removes unused locals or privates by -Ywarn-unused:locals,privates"

  private def removeDeclarationTokens(i: Defn, rhs: Term): Tokens = {
    val startDef = i.tokens.start
    val startBody = rhs.tokens.start
    i.tokens.take(startBody - startDef)
  }

  private def tokensToRemove(defn: Defn): Option[Tokens] = defn match {
    case i @ Defn.Val(_, _, _, Lit(_)) => Some(i.tokens)
    case i @ Defn.Val(_, _, _, rhs) => Some(removeDeclarationTokens(i, rhs))
    case i @ Defn.Var(_, _, _, Some(Lit(_))) => Some(i.tokens)
    case i @ Defn.Var(_, _, _, rhs) => rhs.map(removeDeclarationTokens(i, _))
    case i: Defn.Def => Some(i.tokens)
    case _ => None
  }

  private val unusedPrivateLocalVal: Pattern =
    Pattern.compile("""(local|private) (.*) is never used""")

  def isUnusedPrivate(message: Diagnostic): Boolean =
    unusedPrivateLocalVal.matcher(message.message).matches()

  override def fix(implicit doc: SemanticDoc): Patch = {
    val unusedTerms = {
      doc.diagnostics.collect {
        case message if isUnusedPrivate(message) =>
          message.position
      }.toSet
    }

    def isUnused(defn: Defn) =
      unusedTerms.contains(defn.pos)

    doc.tree.collect {
      case i: Defn if isUnused(i) =>
        tokensToRemove(i)
          .fold(Patch.empty)(Patch.removeTokens)
          .atomic
    }.asPatch
  }
}
