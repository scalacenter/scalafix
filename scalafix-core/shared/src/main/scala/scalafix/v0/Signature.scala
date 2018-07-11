package scalafix.v0

import scala.meta.internal.semanticdb.Scala.{Descriptor => d}

sealed trait Signature {
  def name: String
  def syntax: String
  def structure: String
}

object Signature {
  final case class Type(name: String) extends Signature {
    override def syntax: String = d.Type(name).toString
    override def structure = s"""Signature.Type("$name")"""
    override def toString: String = syntax
  }

  final case class Term(name: String) extends Signature {
    override def syntax: String = d.Term(name).toString
    override def structure = s"""Signature.Term("$name")"""
    override def toString: String = syntax
  }

  final case class Package(name: String) extends Signature {
    override def syntax: String = d.Package(name).toString
    override def structure = s"""Signature.Package("$name")"""
    override def toString: String = syntax
  }

  final case class Method(name: String, disambiguator: String)
      extends Signature {
    @deprecated("Use `disambiguator` instead.", "3.3.0")
    def jvmSignature: String = disambiguator
    override def syntax: String = d.Method(name, disambiguator).toString
    override def structure = s"""Signature.Method("$name", "$disambiguator")"""
    override def toString: String = syntax
  }

  final case class TypeParameter(name: String) extends Signature {
    override def syntax: String = d.TypeParameter(name).toString
    override def structure = s"""Signature.TypeParameter("$name")"""
    override def toString: String = syntax
  }

  final case class TermParameter(name: String) extends Signature {
    override def syntax: String = d.Parameter(name).toString
    override def structure = s"""Signature.TermParameter("$name")"""
    override def toString: String = syntax
  }

  final case class Self(name: String) extends Signature {
    override def syntax =
      throw new UnsupportedOperationException("No longer supported.")
    override def structure = s"""Signature.Self("$name")"""
    override def toString: String = syntax
  }
}
