package scalafix.v0

import scala.meta.internal.{semanticdb => s}

final case class Denotation(
    flags: Long,
    name: String,
    signature: String,
    names: List[ResolvedName]
) extends HasFlags
    with Product
    with Serializable {

  override def toString: String = syntax

  def syntax: String = {
    val s_info = if (signature != "") ": " + signature else ""
    val s_names = ResolvedName.syntax(names)
    val s_name = if (name.contains(" ")) s"`$name`" else name
    s"$flagSyntax $s_name" + s_info + s_names
  }
  def structure = s"""Denotation($flagStructure, "$name", "$signature")"""
}
