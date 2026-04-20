package scala.meta.internal

import scala.meta._
import scala.meta.internal.inputs._
import scala.meta.internal.semanticdb.Scala.Descriptor
import scala.meta.internal.semanticdb.Scala.DescriptorParser
import scala.meta.internal.{semanticdb => s}
import scala.meta.trees.Origin

object ScalametaInternals {
  def symbolOwnerAndDescriptor(symbol: String): (String, Descriptor) = {
    val (desc, owner) = DescriptorParser(symbol)
    (owner, desc)
  }

  def withOrigin[T <: Tree](tree: T, origin: Origin): T =
    tree.withOrigin(origin)

  def positionFromRange(input: Input, range: Option[s.Range]): Position =
    range match {
      case Some(r) => positionFromRange(input, r)
      case _ => Position.None
    }

  def positionFromRange(input: Input, range: s.Range): Position = {
    Position.Range(
      input,
      range.startLine,
      range.startCharacter,
      range.endLine,
      range.endCharacter
    )
  }

  def formatMessage(
      pos: Position,
      severity: String,
      message: String
  ): String = {
    pos.formatMessage(severity, message)
  }
}
