package scalafix.config

import scalafix.util.TreePatch.AddGlobalImport
import scalafix.util.TreePatch.Replace
import scalafix.util.TreePatch
import scala.collection.immutable.Seq
import scalafix.util.ImportPatch
import scalafix.util.TreePatch.RemoveGlobalImport
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
