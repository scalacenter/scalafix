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
  def names: Seq[ResolvedName]
  def entries: Seq[Attributes] = database.entries
  def messages: Seq[Message] = database.messages
  def symbols: Seq[ResolvedSymbol] = database.symbols
  def sugars: Seq[Sugar] = database.sugars

  /** Lookup symbol at this position. */
  def symbol(position: Position): Option[Symbol]

  /** Lookup symbol at this tree.
    *
    * This method returns the same result as symbol(Tree.Position) in most cases
    * but handles some special cases:
    * - when tree is Term/Type.Select(_, name), query by name.position
    * - workaround for https://github.com/scalameta/scalameta/issues/1083
    */
  def symbol(tree: Tree): Option[Symbol]

  /** Lookup denotation of this symbol. */
  def denotation(symbol: Symbol): Option[Denotation]

  /** Lookup denotation of this tree.
    *
    * Shorthand method for symbol(tree).flatMap(denotation).
    */
  def denotation(tree: Tree): Option[Denotation]
}

object SemanticCtx {
  val empty = new SemanticCtxImpl(Database(Nil))
  def apply(entries: Seq[Attributes]): SemanticCtx =
    new SemanticCtxImpl(Database(entries))
  def load(classpath: Classpath): SemanticCtx =
    new SemanticCtxImpl(Database.load(classpath))
  def load(classpath: Classpath, sourcepath: Sourcepath): SemanticCtx =
    new SemanticCtxImpl(Database.load(classpath, sourcepath))
  def load(bytes: Array[Byte]): SemanticCtx =
    new SemanticCtxImpl(Database.load(bytes))
  def load(db: Database): SemanticCtx =
    new SemanticCtxImpl(db)
}
