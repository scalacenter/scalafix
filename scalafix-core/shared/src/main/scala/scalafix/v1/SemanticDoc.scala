package scalafix.v1

import java.nio.file.Files
import java.util
import scala.meta.io.Classpath
import scala.meta.io.RelativePath
import scala.collection.mutable.ListBuffer
import scala.meta._
import scala.meta.contrib.AssociatedComments
import scalafix.internal.util.SymbolTable
import scalafix.util.MatchingParens
import scalafix.util.TokenList
import scala.meta.internal.{semanticdb3 => s}
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
  def symbol(tree: Tree): Sym = {
    val result = symbols(TreePos.symbol(tree))
    if (!result.hasNext) Sym.None
    else result.next() // Discard multi symbols
  }

  def info(sym: Sym): Sym.Info = {
    if (sym.isNone) {
      Sym.Info.empty
    } else if (sym.isLocal) {
      new Sym.Info(
        locals
          .getOrElse(sym.value, throw new NoSuchElementException(sym.value)))
    } else {
      new Sym.Info(
        symtab
          .info(sym.value)
          .getOrElse(throw new NoSuchElementException(sym.value)))
    }
  }

  // ========
  // Privates
  // ========

  private[scalafix] def symbols(pos: Position): Iterator[Sym] = {
    val result = occurrences.getOrDefault(
      s.Range(
        startLine = pos.startLine,
        startCharacter = pos.startColumn,
        endLine = pos.endLine,
        endCharacter = pos.endColumn
      ),
      Nil
    )
    result.iterator.map(Sym(_))

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
    final case class MissingSemanticdb(
        relpath: RelativePath,
        classpath: Classpath)
        extends Error(s"No SemanticDB associated with $relpath")
    final case class MissingTextDocument(
        reluri: String,
        semanticdb: AbsolutePath)
        extends Error(
          s"No TextDocument associated with uri $reluri in $semanticdb")
  }

  def fromPath(
      doc: Doc,
      path: RelativePath,
      classpath: Classpath,
      symtab: SymbolTable
  ): SemanticDoc = {
    val reluri = path.toRelativeURI.toString
    classpath.resolveSemanticdb(path) match {
      case Some(abspath) =>
        val in = Files.newInputStream(abspath.toNIO)
        val sdocs =
          try s.TextDocuments.parseFrom(in).documents
          finally in.close()
        val sdoc = sdocs.find(_.uri == reluri).getOrElse {
          throw Error.MissingTextDocument(reluri, abspath)
        }
        new SemanticDoc(doc, sdoc, symtab)
      case _ =>
        throw Error.MissingSemanticdb(path, classpath)
    }
  }
}
