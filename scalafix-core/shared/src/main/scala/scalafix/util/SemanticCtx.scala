package scalafix.util

import scala.meta._
import scalafix.internal.util.SemanticCtxImpl

/** Context for semantic rules, encapsulates a compilation context.
  *
  * A SemanticCtx is a thin wrapper around [[scala.meta.Database]] with
  * additional in-memory indices for fast Position => Symbol and
  * Symbol => Denotation lookups.
  */
trait SemanticCtx {

  /** List of source files that built this SemanticCtx. */
  def sourcepath: Sourcepath

  /** Classpath built this SemanticCtx. */
  def classpath: Classpath

  /** The underlying raw database. */
  def database: Database

  /** Shorthand for scala.meta.Database.documents */
  def documents: Seq[Document] = database.documents

  /** Shorthand for scala.meta.Database.messages */
  def messages: Seq[Message] = database.messages

  /** Shorthand for scala.meta.Database.symbols */
  def symbols: Seq[ResolvedSymbol] = database.symbols

  /** Shorthand for scala.meta.Database.synthetics */
  def synthetics: Seq[Synthetic] = database.synthetics

  /** The resolved names in this database.
    *
    * Includes resolved name in synthetics, such as inferred implicits/types.
    */
  def names: Seq[ResolvedName]

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

  /** Build new SemanticCtx with only these documents. */
  def withDocuments(documents: Seq[Document]): SemanticCtx

  object Symbol {
    def unapply(tree: Tree): Option[Symbol] = symbol(tree)
    def unapply(pos: Position): Option[Symbol] = symbol(pos)
  }
}

object SemanticCtx {
  val empty: SemanticCtx =
    SemanticCtxImpl(Database(Nil), Sourcepath(Nil), Classpath(Nil))
  def load(classpath: Classpath): SemanticCtx =
    SemanticCtxImpl(Database.load(classpath), Sourcepath(Nil), classpath)
  def load(sourcepath: Sourcepath, classpath: Classpath): SemanticCtx =
    SemanticCtxImpl(Database.load(classpath, sourcepath), sourcepath, classpath)
  def load(
      database: Database,
      sourcepath: Sourcepath,
      classpath: Classpath): SemanticCtx =
    SemanticCtxImpl(database, sourcepath, classpath)
  def load(bytes: Array[Byte]): SemanticCtx =
    empty.withDocuments(Database.load(bytes).documents)
}
