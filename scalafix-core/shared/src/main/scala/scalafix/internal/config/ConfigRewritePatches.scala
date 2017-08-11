package scalafix.internal.config

import scalafix.Patch
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.ReplaceSymbol
import scalafix.patch.TreePatch.RemoveGlobalImport
import metaconfig.ConfDecoder

case class ConfigRewritePatches(
    replaceSymbols: List[ReplaceSymbol] = Nil,
    addGlobalImports: List[AddGlobalImport] = Nil,
    removeGlobalImports: List[RemoveGlobalImport] = Nil
) {
  val reader: ConfDecoder[ConfigRewritePatches] =
    ConfDecoder.instanceF[ConfigRewritePatches] { conf =>
      import conf._
      (
        getOrElse("replaceSymbols")(replaceSymbols) |@|
          getOrElse("addGlobalImports")(addGlobalImports) |@|
          getOrElse("removeGlobalImports")(removeGlobalImports)
      ).map { case ((a, b), c) => ConfigRewritePatches(a, b, c) }
    }
  def all: List[Patch] =
    replaceSymbols ++ addGlobalImports ++ removeGlobalImports
}

object ConfigRewritePatches {
  val default = ConfigRewritePatches()
}
