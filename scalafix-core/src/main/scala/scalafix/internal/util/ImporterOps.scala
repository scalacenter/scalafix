package scalafix.internal.util

import scala.meta._

object ImporterOps {

  def importerOf(tree: Tree): Option[Importer] = tree.parent match {
    case Some(x: Importer) => Some(x)
    case _ => None
  }

  implicit class ImplicitImporter(val self: Importer) extends AnyVal {
    def values: List[Importee] = self.importees
    def withValues(importee: Importee): Importer = withValues(importee :: Nil)
    def withValues(importees: List[Importee]): Importer =
      self.copy(importees = importees)
    def withRef(ref: Term.Ref): Importer = self.copy(ref = ref)
  }

  implicit class ImplicitTermRef(val self: Term.Ref) extends AnyVal {
    def importer(importee: Importee): Importer = importer(importee :: Nil)
    def importer(importees: List[Importee]): Importer =
      Importer(self, importees)
  }

}
