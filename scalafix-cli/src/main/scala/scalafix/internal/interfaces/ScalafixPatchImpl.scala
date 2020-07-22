package scalafix.internal.interfaces

import java.util.UUID

import scalafix.interfaces.{ScalafixPatch}
import scalafix.{Patch, v0}

import scala.util.Try

case class ScalafixPatchImpl(id: ScalafixPatchImpl.Id, patch: Patch)
    extends ScalafixPatch {
  override def kind(): String = patch.getClass.getCanonicalName

  override def getId: String = id.value

}

object ScalafixPatchImpl {

  case class Id(value: String) {
    assert(Id.isValid(value), s"Invalid format for ${getClass.getName}: $value")

    override def toString: String = value
  }

  object Id {
    def generate(): Id = Id(UUID.randomUUID().toString)

    def isValid(in: String): Boolean = Try(UUID.fromString(in)).isSuccess

    def from(in: String): Try[Id] = Try(Id(in))
  }

  def from(patch: Patch): ScalafixPatchImpl =
    ScalafixPatchImpl(Id.generate(), patch)

}
