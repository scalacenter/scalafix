package scalafix

import scala.meta._
import scala.meta.semantic.v1.Completed
import scala.meta.semantic.v1.Symbol
import scala.meta.tokens.Token
import scala.util.Try
import scalafix.util.CanonicalImport
import scalafix.util.ImportPatch
import scalafix.util.logger

import com.typesafe.config.Config

package object syntax {
  implicit class XtensionImporter(i: CanonicalImport) {
    def supersedes(patch: ImportPatch): Boolean =
      i.ref.structure == patch.importer.ref.structure &&
        (i.importee.is[Importee.Wildcard] ||
          i.importee.structure == patch.importee.structure)
  }

  implicit class XtensionToken(token: Token) {
    def posTuple: (Int, Int) = token.start -> token.end
  }

  implicit class XtensionConfig(config: Config) {
    def getBoolOrElse(key: String, els: Boolean): Boolean =
      if (config.hasPath(key)) config.getBoolean(key)
      else els
  }
  implicit class XtensionCompleted[T](completed: Completed[T]) {
    def toOption: Option[T] = completed match {
      case Completed.Success(e) => Some(e)
      case _ => None
    }
  }
  implicit class XtensionSymbol(symbol: Symbol) {
    def toTermRef: Option[Term.Ref] =
      Try {
        symbol.syntax
          .stripPrefix("_root_.")
          .stripSuffix(".")
          .parse[Term]
          .get
          .asInstanceOf[Term.Ref]
      }.toOption
  }
  implicit class XtensionString(str: String) {
    def reveal: String = logger.reveal(str)
  }
}
