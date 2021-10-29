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
import scalafix.util.TreeOps
import scalafix.v1.SemanticTree
import scalafix.v1.Symbol
import scalafix.v1.SymbolInformation
import scalafix.v1.Symtab
import scalafix.v1.SyntacticDocument

final class InternalSemanticDoc(
    val doc: SyntacticDocument,
    _textDocument: LazyValue[s.TextDocument],
    val symtab: SymbolTable
) extends Symtab {
  def this(
      doc: SyntacticDocument,
      textDocument: s.TextDocument,
      symtab: SymbolTable
  ) = this(doc, LazyValue.now(textDocument), symtab)

  def textDocument: s.TextDocument = {
    _textDocument.value
  }

  def synthetics: Iterator[SemanticTree] =
    textDocument.synthetics.iterator.map { tree =>
      DocumentFromProtobuf.convert(tree, this)
    }
  def messages: Iterator[Diagnostic] =
    textDocument.diagnostics.iterator.map { diag =>
      SemanticdbDiagnostic(doc.input, diag)
    }

  def synthetic(pos: Position): List[SemanticTree] = {
    val synth = _synthetics.getOrDefault(
      s.Range(pos.startLine, pos.startColumn, pos.endLine, pos.endColumn),
      List.empty
    )
    synth.map(DocumentFromProtobuf.convert(_, this))
  }

  def symbol(tree: Tree): Symbol = {
    def fromTextDocument() = {
      val result =
        TreePos.symbolImpl[Iterator[Symbol]](tree)(symbols, _.isEmpty)
      if (result.hasNext) result.next() // Discard multi symbols
      else Symbol.None
    }
    if (!_textDocument.isEvaluated) {
      TreeOps.inferGlobalSymbol(tree).getOrElse(fromTextDocument())
    } else {
      fromTextDocument()
    }
  }

  def info(sym: Symbol): Option[SymbolInformation] = {
    if (sym.isNone) {
      None
    } else if (sym.isLocal) {
      locals.get(sym.value).map(new SymbolInformation(_)(this))
    } else {
      val fromGlobalSymtab =
        symtab.info(sym.value).map(new SymbolInformation(_)(this))
      def fromTextDocument(): Option[SymbolInformation] =
        textDocument.symbols
          .find(_.symbol == sym.value)
          .map(new SymbolInformation(_)(this))
      fromTextDocument().orElse(fromGlobalSymtab)
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

  private[this] lazy val locals = textDocument.symbols.iterator.collect {
    case info
        if info.symbol.startsWith("local") ||
          info.symbol.contains(
            "$anon"
          ) // NOTE(olafur) workaround for a semanticdb-scala issue.
        =>
      info.symbol -> info
  }.toMap

  private[this] lazy val _synthetics: util.Map[s.Range, List[s.Synthetic]] = {
    val result = new util.HashMap[s.Range, List[s.Synthetic]]()
    textDocument.synthetics.foreach { synthetic =>
      if (synthetic.range.isDefined) {
        val key = synthetic.range.get
        val currentValue = result.getOrDefault(key, List.empty[s.Synthetic])
        result.put(key, synthetic :: currentValue)
      }
    }
    result
  }
  private[this] lazy val occurrences
      : util.Map[s.Range, collection.Seq[String]] = {
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
    result.asInstanceOf[util.Map[s.Range, collection.Seq[String]]]
  }

}
