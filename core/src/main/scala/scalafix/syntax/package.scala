package scalafix

import scala.meta.Importee
import scala.meta.tokens.Token
import scalafix.util.CanonicalImport
import scalafix.util.ImportPatch
import scalafix.util.logger

import com.typesafe.config.Config

package object syntax {
  implicit class XtensionImporter(i: CanonicalImport) {
    def supersedes(patch: ImportPatch): Boolean =
      i.ref.structure == patch.importer.ref.structure &&
        (i.importee.is[Importee.Wildcard] ||
          i.importee.structure == patch.importer.importee.structure)
  }

  implicit class XtensionToken(token: Token) {
    def posTuple: (Int, Int) = token.start -> token.end
  }

  implicit class XtensionConfig(config: Config) {
    def getBoolOrElse(key: String, els: Boolean): Boolean =
      if (config.hasPath(key)) config.getBoolean(key)
      else els
  }
  implicit class XtensionString(str: String) {
    def reveal: String = logger.reveal(str)
  }
}
