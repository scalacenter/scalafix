package scalafix.util

import scala.meta._
import scalafix.internal.util.ScalafixMirror

trait Mirror {
  def database: Database
  def entries: Seq[Attributes] = database.entries
  def names: Seq[ResolvedName] = database.names
  def messages: Seq[Message] = database.messages
  def symbols: Seq[ResolvedSymbol] = database.symbols
  def sugars: Seq[Sugar] = database.sugars
  def symbol(position: Position): Option[Symbol]
  def denotation(symbol: Symbol): Option[Denotation]
}

object Mirror {
  def apply(entries: Seq[Attributes]): Mirror =
    new ScalafixMirror(Database(entries))
  def load(classpath: Classpath): Mirror =
    new ScalafixMirror(Database.load(classpath))
  def load(classpath: Classpath, sourcepath: Sourcepath): Mirror =
    new ScalafixMirror(Database.load(classpath, sourcepath))
  def load(bytes: Array[Byte]): Mirror =
    new ScalafixMirror(Database.load(bytes))
}
