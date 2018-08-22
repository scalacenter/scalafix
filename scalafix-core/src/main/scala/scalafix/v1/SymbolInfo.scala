package scalafix.v1

import scala.meta.internal.{semanticdb => s}
import scalafix.internal.v1.FromProtobuf

object SymbolInfo {
  val empty = new SymbolInfo(s.SymbolInformation())
}

final class SymbolInfo private[scalafix] (
    private[scalafix] val info: s.SymbolInformation
) {
  def isNone: Boolean = info.symbol.isEmpty
  def sym: Symbol = Symbol(info.symbol)
  def owner: Symbol = Symbol(info.symbol).owner
  def name: String = info.name
  def kind: SymbolKind = new SymbolKind(info)
  def signature(implicit doc: SemanticDoc): Signature =
    new FromProtobuf(doc).ssignature(info.signature)
  def props: SymbolProperties = new SymbolProperties(info.properties)
  def access: SymbolAccess = FromProtobuf.saccess(info.access)

  override def toString: String = s"SymbolInfo(${info.symbol})"
}
