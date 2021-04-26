package banana.rule

import scala.meta._
import scala.meta.contrib._

import scalafix.patch.Patch
import scalafix.v1._

class FqnRule extends SemanticRule("FqnRule") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    Patch.addGlobalImport(importer"scala.collection.immutable") + {
      val fqnRule = SymbolMatcher.exact("test/FqnRule.")
      doc.tree.collect { case fqnRule(t: Term.Name) =>
        Patch.addLeft(t, "/* matched */ ")
      }.asPatch
    }

  }
}

case object FqnRule2 extends SyntacticRule("FqnRule2") {
  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree.collectFirst { case n: Name =>
      Patch.replaceTree(n, n.value + "2")
    }.asPatch
}

class PatchTokenWithEmptyRange
    extends SyntacticRule("PatchTokenWithEmptyRange") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tokens.collect {
      case tok @ Token.Interpolation.SpliceEnd() =>
        Patch.addRight(tok, "a")
      case tok @ Token.Xml.SpliceEnd() =>
        Patch.addRight(tok, "a")
    }
  }.asPatch
}

class SemanticRuleV1 extends SemanticRule("SemanticRuleV1") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    Patch.addRight(doc.tree, "\nobject SemanticRuleV1")
  }
}

class SyntacticRuleV1 extends SyntacticRule("SyntacticRuleV1") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    Patch.addRight(doc.tree, "\nobject SyntacticRuleV1")
  }
}

class CommentFileNonAtomic extends SyntacticRule("CommentFileNonAtomic") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    Patch.addLeft(doc.tree, "/*") +
      Patch.addRight(doc.tree, "*/")
  }
}

class CommentFileAtomic extends SyntacticRule("CommentFileAtomic") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    (Patch.addLeft(doc.tree, "/*") +
      Patch.addRight(doc.tree, "*/")).atomic
  }
}
