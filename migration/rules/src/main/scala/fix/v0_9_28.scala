package fix

import scala.meta._

import scalafix.v1._

class v0_9_28 extends SemanticRule("v0_9_28") {
  override def fix(implicit doc: SemanticDocument): Patch =
    doc.tree.collect {
      case t @ Importee.Name(Name("SemanticRuleSuite")) =>
        Patch.removeImportee(t) +
          Patch.addGlobalImport(
            importer"scalafix.testkit.AbstractSemanticRuleSuite"
          )
      case t @ Init(_, _, _) =>
        if (t.toString() == "SemanticRuleSuite()")
          Patch.addGlobalImport(importer"org.scalatest.FunSuiteLike") +
            Patch.replaceTree(t, "AbstractSemanticRuleSuite") +
            Patch.addRight(t, " with FunSuiteLike")
        else Patch.empty
    }.asPatch
}
