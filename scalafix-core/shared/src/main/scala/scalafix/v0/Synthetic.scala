package scalafix.v0

import scala.meta.inputs._
import scala.meta.internal.inputs._

final case class Synthetic(
    position: Position,
    text: String,
    names: List[ResolvedName]) {
  def syntax: String = {
    val s_names = ResolvedName.syntax(names)
    s"[${position.start}..${position.end}): $text$s_names"
  }
  def structure =
    s"""Synthetic(${position.structure}, "$text", List(${names
      .map(_.structure)
      .mkString(", ")}))"""
  override def toString = syntax
}
