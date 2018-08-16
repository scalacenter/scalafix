package scalafix.internal.util

import scala.collection.mutable
import scala.meta._
import scala.meta.internal.{semanticdb => s}
import scala.meta.internal.symtab._
import scalafix.internal.v0._
import scalafix.util.SemanticdbIndex
import scalafix.v0._
import scalafix.v0

case class EagerInMemorySemanticdbIndex(
    database: Database,
    classpath: Classpath,
    table: SymbolTable = AggregateSymbolTable(Nil)
) extends SemanticdbIndex
    with SymbolTable {
  override def toString: String =
    s"$productPrefix($classpath, database.size=${database.documents.length})"
  override def hashCode(): Int = database.hashCode()
  private lazy val _denots: mutable.Map[Symbol, Denotation] = {
    val builder = mutable.Map.empty[Symbol, Denotation]
    database.symbols.foreach(r => builder += (r.symbol -> r.denotation))
    builder.result()
  }
  private lazy val _names: mutable.Map[Position, ResolvedName] = {
    val builder = mutable.Map.empty[Position, ResolvedName]
    def add(r: ResolvedName): Unit = {
      builder.get(r.position) match {
        case Some(conflict) =>
          conflict.symbol match {
            case v0.Symbol.Multi(syms) =>
              builder(r.position) =
                conflict.copy(symbol = v0.Symbol.Multi(r.symbol :: syms))
            case sym =>
              builder(r.position) =
                conflict.copy(symbol = v0.Symbol.Multi(r.symbol :: sym :: Nil))
          }
        case _ =>
          builder(r.position) = r
      }
    }
    database.documents.foreach { entry =>
      entry.names.foreach(add)
      entry.synthetics.foreach(_.names.foreach(add))
      entry.symbols.foreach(_.denotation.names.foreach(add))
    }
    builder.result()
  }
  def symbol(position: Position): Option[Symbol] =
    _names.get(position).map(_.symbol)
  def symbol(tree: Tree): Option[Symbol] = tree match {
    case name @ Name(_) =>
      val syntax = name.syntax
      // workaround for https://github.com/scalameta/scalameta/issues/1083
      val pos =
        if (syntax.startsWith("(") &&
          syntax.endsWith(")") &&
          syntax != name.value)
          Position.Range(name.pos.input, name.pos.start + 1, name.pos.end - 1)
        else name.pos
      symbol(pos)
    case Importee.Rename(name, _) => symbol(name)
    case Importee.Name(name) => symbol(name)
    case Term.Select(_, name @ Name(_)) => symbol(name)
    case Type.Select(_, name @ Name(_)) => symbol(name)
    case _ => symbol(tree.pos)
  }
  def denotation(symbol: Symbol): Option[Denotation] =
    _denots.get(symbol)
  def denotation(tree: Tree): Option[Denotation] =
    symbol(tree).flatMap(denotation)
  override def names: Seq[ResolvedName] = _names.values.toSeq
  def withDocuments(documents: Seq[Document]): SemanticdbIndex =
    copy(database = Database(documents))

  private def denotationToSymbolInformation(
      symbol: String,
      denot: Denotation,
      owner: String
  ): s.SymbolInformation = {
    s.SymbolInformation(
      symbol = symbol,
      language = s.Language.SCALA,
      kind = s.SymbolInformation.Kind.fromValue(denot.skind.value),
      properties = denot.sproperties,
      name = denot.name
    )
  }

  override def info(symbol: String): Option[s.SymbolInformation] =
    table.info(symbol).orElse {
      val msym = v0.Symbol(symbol)
      denotation(msym).map(denot =>
        denotationToSymbolInformation(symbol, denot, {
          msym match {
            case v0.Symbol.Global(owner, _) => owner.syntax
            case _ => ""
          }
        }))
    }
}
