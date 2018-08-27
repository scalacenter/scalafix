package scalafix.internal.rule

import metaconfig.Configured
import scala.meta._
import scalafix.v1._

class RemoveUnusedImports extends SemanticRule("RemoveUnusedImports") {

  override def description: String =
    "Rewrite that removes unused imports reported by the compiler under -Xwarn-unused-import."

  override def withConfiguration(config: Configuration): Configured[Rule] =
    if (config.scalaVersion.startsWith("2.11") &&
      !config.scalacOptions.contains("-Ywarn-unused-import")) {
      Configured.error(
        s"The compiler option -Ywarn-unused-import is required to use RemoveUnusedImports. " +
          s"Obtained ${config.scalacOptions}")
    } else {
      Configured.ok(this)
    }

  override def fix(implicit doc: SemanticDoc): Patch = {
    val unusedImports = doc.diagnostics.toIterator.collect {
      case message if message.message == "Unused import" =>
        message.position
    }.toSet
    def isUnused(importee: Importee): Boolean = {
      val pos = importee match {
        case Importee.Rename(name, _) => name.pos
        case _ => importee.pos
      }
      unusedImports.contains(pos)
    }
    doc.tree.collect {
      case i: Importee if isUnused(i) => Patch.removeImportee(i).atomic
    }.asPatch
  }
}
