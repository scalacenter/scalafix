package scalafix.internal.config

import scalafix.Patch
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.ReplaceSymbol
import scalafix.patch.TreePatch.RemoveGlobalImport
import metaconfig.ConfDecoder

case class ConfigRulePatches(
    replaceSymbols: List[ReplaceSymbol] = Nil,
    addGlobalImports: List[AddGlobalImport] = Nil,
    removeGlobalImports: List[RemoveGlobalImport] = Nil
) {
  val reader: ConfDecoder[ConfigRulePatches] =
    ConfDecoder.instanceF[ConfigRulePatches] { conf =>
      import conf._
      (
        getOrElse("replaceSymbols")(replaceSymbols) |@|
          getOrElse("addGlobalImports")(addGlobalImports) |@|
          getOrElse("removeGlobalImports")(removeGlobalImports)
      ).map { case ((a, b), c) => ConfigRulePatches(a, b, c) }
    }
  def all: List[Patch] =
    replaceSymbols ++ addGlobalImports ++ removeGlobalImports
}

object ConfigRulePatches {
  val default: ConfigRulePatches = ConfigRulePatches()
}
