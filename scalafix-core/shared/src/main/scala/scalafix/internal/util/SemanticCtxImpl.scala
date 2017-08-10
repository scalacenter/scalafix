package scalafix
package internal.util

import scala.meta._

class SemanticCtxImpl(val database: Database) extends SemanticCtx {
  override def toString: String = database.toString()
  override def hashCode(): Int = database.hashCode()
  private val _denots: Map[Symbol, Denotation] = {
    val builder = Map.newBuilder[Symbol, Denotation]
    database.symbols.foreach(r => builder += (r.sym -> r.denot))
    builder.result()
  }
  private val _names: Map[Position, ResolvedName] = {
    val builder = Map.newBuilder[Position, ResolvedName]
    def add(r: ResolvedName) = {
      builder += (r.pos -> r)
    }
    database.entries.foreach { entry =>
      entry.names.foreach(add)
      entry.sugars.foreach(sugar => sugar.names.foreach(add))
    }
    builder.result()
  }

  def symbol(position: Position): Option[Symbol] =
    _names.get(position).map(_.sym)
  def denotation(symbol: Symbol): Option[Denotation] =
    _denots.get(symbol)
}
