package scalafix.internal.config

import scalafix.patch.Patch
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.ReplaceSymbol
import scalafix.patch.TreePatch.RemoveGlobalImport
import metaconfig.ConfDecoder
import metaconfig.generic
import metaconfig.generic.Surface

case class ConfigRulePatches(
    replaceSymbols: List[ReplaceSymbol] = Nil,
    addGlobalImports: List[AddGlobalImport] = Nil,
    removeGlobalImports: List[RemoveGlobalImport] = Nil
) {
  def all: List[Patch] =
    replaceSymbols ++ addGlobalImports ++ removeGlobalImports
}

object ConfigRulePatches {
  implicit val surface: Surface[ConfigRulePatches] =
    generic.deriveSurface[ConfigRulePatches]
  val default: ConfigRulePatches = ConfigRulePatches()
  implicit val configRuleDecoder: ConfDecoder[ConfigRulePatches] =
    generic.deriveDecoder[ConfigRulePatches](default)
}
