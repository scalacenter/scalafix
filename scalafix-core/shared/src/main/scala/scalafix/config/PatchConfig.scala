package scalafix.config

import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.Replace
import scalafix.patch.TreePatch
import scalafix.patch.TreePatch.MoveSymbol
import scalafix.patch.TreePatch.RemoveGlobalImport
import metaconfig._

@DeriveConfDecoder
case class PatchConfig(
    removeGlobalImports: List[RemoveGlobalImport] = Nil,
    addGlobalImports: List[AddGlobalImport] = Nil,
    moveSymbols: List[MoveSymbol] = Nil,
    replacements: List[Replace] = Nil
) {
  def all: Seq[TreePatch] =
    removeGlobalImports ++
      addGlobalImports ++
      replacements
}

object PatchConfig {
  def default = PatchConfig()
}
