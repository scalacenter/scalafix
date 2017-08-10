package scalafix.util

import scala.meta._
import scalafix.internal.util.SemanticCtxImpl

/** Context for semantic rewrites.
  *
  * A SemanticCtx is a thin wrapper around [[scala.meta.Database]] with
  * additional in-memory indices for fast Position => Symbol and
  * Symbol => Denotation lookups.
  */
trait SemanticCtx {
  def database: Database
  def entries: Seq[Attributes] = database.entries
  def names: Seq[ResolvedName] = database.names
  def messages: Seq[Message] = database.messages
  def symbols: Seq[ResolvedSymbol] = database.symbols
  def sugars: Seq[Sugar] = database.sugars
  /** Lookup symbol at this position. */
  def symbol(position: Position): Option[Symbol]
  /** Lookup denotation of this symbol. */
  def denotation(symbol: Symbol): Option[Denotation]
}

object SemanticCtx {
  def apply(entries: Seq[Attributes]): SemanticCtx =
    new SemanticCtxImpl(Database(entries))
  def load(classpath: Classpath): SemanticCtx =
    new SemanticCtxImpl(Database.load(classpath))
  def load(classpath: Classpath, sourcepath: Sourcepath): SemanticCtx =
    new SemanticCtxImpl(Database.load(classpath, sourcepath))
  def load(bytes: Array[Byte]): SemanticCtx =
    new SemanticCtxImpl(Database.load(bytes))
}
