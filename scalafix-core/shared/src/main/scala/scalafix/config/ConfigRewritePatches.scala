package scalafix.config

import scalafix.Patch
import scalafix.patch.TreePatch.AddGlobalImport
import scalafix.patch.TreePatch.MoveSymbol
import scalafix.patch.TreePatch.RemoveGlobalImport
import metaconfig.ConfDecoder

case class ConfigRewritePatches(
    moveSymbols: List[MoveSymbol] = Nil,
    addGlobalImports: List[AddGlobalImport] = Nil,
    removeGlobalImports: List[RemoveGlobalImport] = Nil
) {
  val reader: ConfDecoder[ConfigRewritePatches] =
    ConfDecoder.instanceF[ConfigRewritePatches] { conf =>
      import conf._
      (
        getOrElse("moveSymbols")(moveSymbols) |@|
          getOrElse("addGlobalImports")(addGlobalImports) |@|
          getOrElse("removeGlobalImports")(removeGlobalImports)
      ).map { case ((a, b), c) => ConfigRewritePatches(a, b, c) }
    }
  def all: List[Patch] = moveSymbols ++ addGlobalImports ++ removeGlobalImports
}

object ConfigRewritePatches {
  val default = ConfigRewritePatches()
}
