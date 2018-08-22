package scalafix.v1

import java.util
import scala.meta.io.RelativePath
import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.internal.symtab.SymbolTable
import scalafix.util.MatchingParens
import scalafix.util.TokenList
import scala.meta.internal.{semanticdb => s}
import scalafix.internal.patch.DocSemanticdbIndex
import scalafix.internal.v1._
import scalafix.rule.RuleCtx
import scalafix.util.SemanticdbIndex

final class SemanticDoc private[scalafix] (
    val doc: Doc,
    // privates
    private[scalafix] val sdoc: s.TextDocument,
    private[scalafix] val symtab: SymbolTable
) extends SemanticContext {

  override def toString: String = s"SemanticDoc(${input.syntax})"

  // ==========
  // Legacy API
  // ==========
  def toRuleCtx: RuleCtx = doc.toRuleCtx
  def toSemanticdbIndex: SemanticdbIndex = new DocSemanticdbIndex(this)

  // =============
  // Syntactic API
  // =============
  def tree: Tree = doc.tree
  def tokens: Tokens = doc.tokens
  def input: Input = doc.input
  def matchingParens: MatchingParens = doc.matchingParens
  def tokenList: TokenList = doc.tokenList
  def comments: AssociatedComments = doc.comments

  // ============
  // Semantic API
  // ============
  def symbol(tree: Tree): Symbol = {
    val result = symbols(TreePos.symbol(tree))
    if (!result.hasNext) Symbol.None
    else result.next() // Discard multi symbols
  }

  def info(sym: Symbol): SymbolInfo = {
    if (sym.isNone) {
      SymbolInfo.empty
    } else if (sym.isLocal) {
      new SymbolInfo(
        locals.getOrElse(sym.value, throw new MissingSymbolException(sym))
      )
    } else {
      symtab.info(sym.value) match {
        case Some(x) => new SymbolInfo(x)
        case _ => SymbolInfo.empty
      }
    }
  }

  // ========
  // Privates
  // ========

  private[scalafix] def symbols(pos: Position): Iterator[Symbol] = {
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
  private[scalafix] def config = doc.config
  private[scalafix] val locals = sdoc.symbols.iterator.collect {
    case info
        if info.symbol.startsWith("local") ||
          info.symbol.contains("$anon") // NOTE(olafur) workaround for a semanticdb-scala issue.
        =>
      info.symbol -> info
  }.toMap

  private[scalafix] val occurrences: util.Map[s.Range, Seq[String]] = {
    val result = new util.HashMap[s.Range, ListBuffer[String]]()
    sdoc.occurrences.foreach { o =>
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

object SemanticDoc {
  sealed abstract class Error(msg: String) extends Exception(msg)
  object Error {
    final case class MissingSemanticdb(reluri: String)
        extends Error(s"SemanticDB not found: $reluri")
    final case class MissingTextDocument(reluri: String)
        extends Error(s"TextDocument.uri not found: $reluri")
  }

  def fromPath(
      doc: Doc,
      path: RelativePath,
      classLoader: ClassLoader,
      symtab: SymbolTable
  ): SemanticDoc = {
    val semanticdbReluri = s"META-INF/semanticdb/$path.semanticdb"
    Option(classLoader.getResourceAsStream(semanticdbReluri)) match {
      case Some(inputStream) =>
        val sdocs =
          try s.TextDocuments.parseFrom(inputStream).documents
          finally inputStream.close()
        val reluri = path.toRelativeURI.toString
        val sdoc = sdocs.find(_.uri == reluri).getOrElse {
          throw Error.MissingTextDocument(reluri)
        }
        new SemanticDoc(doc, sdoc, symtab)
      case None =>
        throw Error.MissingSemanticdb(semanticdbReluri)
    }
  }
}
