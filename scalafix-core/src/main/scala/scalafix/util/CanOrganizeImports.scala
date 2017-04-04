package scalafix.util

import scala.meta._
import scalafix.syntax._
import scalafix.rewrite.ScalafixMirror

object CanOrganizeImports {
  // Not implicit to avoid implicit ambiguity
  val ScalafixMirror: CanOrganizeImports[ScalafixMirror] =
    new CanOrganizeImports[ScalafixMirror] {
      override def toOrganizeImportsMirror(
          e: ScalafixMirror): OrganizeImportsMirror =
        new OrganizeImportsMirror {
          override def fullyQualifiedName(ref: Ref): Option[Ref] = e.fqn(ref)
          override def isUnused(importee: Importee): Boolean =
            e.isUnusedImport(importee)
        }
    }

  implicit val ScalaMetaMirrorCanOrganizeImports: CanOrganizeImports[Mirror] =
    new CanOrganizeImports[Mirror] {
      override def toOrganizeImportsMirror(e: Mirror): OrganizeImportsMirror =
        new OrganizeImportsMirror {
          override def fullyQualifiedName(ref: Ref): Option[Ref] =
            for {
              sym <- e.symbol(ref).toOption
              name <- sym.to[Ref].toOption
              strippedRoot = name.transform {
                case q"_root_.$nme" => nme
              }
              if strippedRoot.is[Ref]
            } yield strippedRoot.asInstanceOf[Ref]
          // scala.meta.Miror does not support isUnusedImport
          override def isUnused(importee: Importee): Boolean = false
        }
    }
}

trait CanOrganizeImports[T] {
  def toOrganizeImportsMirror(e: T): OrganizeImportsMirror
}
