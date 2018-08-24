package scalafix.internal.v1

import java.util
import scala.collection.mutable.ListBuffer
import scala.meta.Position
import scala.meta.Tree
import scala.meta.internal.metap.PrinterSymtab
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => s}
import scalafix.internal.config.ScalafixConfig
import scalafix.lint.Diagnostic
import scalafix.v1.Doc
import scalafix.v1.Symbol
import scalafix.v1.SymbolInfo
import scalafix.v1.Symtab

final class InternalSemanticDoc(
    val doc: Doc,
    val textDocument: s.TextDocument,
    val symtab: SymbolTable
) extends Symtab {
  def messages: List[Diagnostic] =
    textDocument.diagnostics.iterator.map { diag =>
      SemanticdbDiagnostic(doc.input, diag)
    }.toList

  def symbol(tree: Tree): Symbol = {
    val result = symbols(TreePos.symbol(tree))
    if (result.hasNext) result.next() // Discard multi symbols
    else Symbol.None
  }

  def info(sym: Symbol): Option[SymbolInfo] = {
    if (sym.isNone) {
      None
    } else if (sym.isLocal) {
      locals.get(sym.value).map(new SymbolInfo(_)(this))
    } else {
      symtab.info(sym.value).map(new SymbolInfo(_)(this))
    }
  }

  def symbols(pos: Position): Iterator[Symbol] = {
    val result = occurrences.getOrDefault(
      s.Range(
        startLine = pos.startLine,
        startCharacter = pos.startColumn,
        endLine = pos.endLine,
        endCharacter = pos.endColumn
      ),
      Nil
    )
    result.iterator.map(Symbol(_))
  }

  def config: ScalafixConfig = doc.internal.config

  def printerSymtab: PrinterSymtab =
    new scala.meta.internal.metap.PrinterSymtab {
      def info(symbol: String): Option[s.SymbolInformation] =
        symtab.info(symbol)
    }

  private[this] val locals = textDocument.symbols.iterator.collect {
    case info
        if info.symbol.startsWith("local") ||
          info.symbol.contains("$anon") // NOTE(olafur) workaround for a semanticdb-scala issue.
        =>
      info.symbol -> info
  }.toMap

  private[this] val occurrences: util.Map[s.Range, Seq[String]] = {
    val result = new util.HashMap[s.Range, ListBuffer[String]]()
    textDocument.occurrences.foreach { o =>
      if (o.range.isDefined) {
        val key = o.range.get
        var buffer = result.get(key)
        if (buffer == null) {
          buffer = ListBuffer.empty[String]
          result.put(key, buffer)
        }
        buffer += o.symbol
      }
    }
    result.asInstanceOf[util.Map[s.Range, Seq[String]]]
  }

}
