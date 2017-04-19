package scalafix.config

import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.Replace
import scalafix.patch.TreePatch
import scala.collection.immutable.Seq
import scalafix.patch.ImportPatch
import scalafix.patch.TreePatch.RemoveGlobalImport
import metaconfig._

@DeriveConfDecoder
case class PatchConfig(
    removeGlobalImports: List[RemoveGlobalImport] = Nil,
    addGlobalImports: List[AddGlobalImport] = Nil,
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
