package scalafix.internal.util

import org.langmeta.semanticdb.Symbol
import scala.meta.Denotation
import scalafix.SemanticdbIndex
import org.langmeta.internal.semanticdb._
import scala.meta.internal.{semanticdb3 => s}

/**
  * Combination of a SymbolTable and SemanticdbIndex.
  *
  * This utility is necessary because SymbolTable contains only global symbols and
  * SemanticdbIndex has local symbols.
  */
class CombinedSymbolTable(index: SemanticdbIndex, table: SymbolTable) {
  private def denotationToSymbolInformation(
      symbol: String,
      denot: Denotation): s.SymbolInformation = {
    s.SymbolInformation(
      symbol = symbol,
      language = s.Language.SCALA,
      kind = s.SymbolInformation.Kind.fromValue(denot.skind.value),
      properties = denot.sproperties,
      name = denot.name,
      tpe = denot.tpeInternal
    )
  }
  def info(symbol: String): s.SymbolInformation =
    table.info(symbol).getOrElse {
      denotationToSymbolInformation(
        symbol,
        index
          .denotation(Symbol(symbol))
          .getOrElse(throw new NoSuchElementException(symbol))
      )
    }
}
