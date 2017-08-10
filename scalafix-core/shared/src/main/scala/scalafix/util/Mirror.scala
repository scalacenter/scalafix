package scalafix.util

import scala.meta._

trait Mirror {
  def database: Database
  def symbol(position: Position): Option[Symbol]
  def denotation(symbol: Symbol): Option[Denotation]
}
