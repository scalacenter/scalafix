package scalafix.v1

import scala.meta._
import scala.meta.contrib.AssociatedComments
import scala.meta.internal.symtab.SymbolTable
import scala.meta.internal.{semanticdb => s}
import scala.meta.io.RelativePath
import scalafix.internal.v1._
import scalafix.util.MatchingParens
import scalafix.util.TokenList

final class SemanticDocument private[scalafix] (
    private[scalafix] val internal: InternalSemanticDoc
) extends SemanticContext
    with Symtab {

  def tree: Tree =
    internal.doc.internal.tree.value
  def tokens: Tokens =
    tree.tokens
  def input: Input =
    internal.doc.internal.input

  def matchingParens: MatchingParens =
    internal.doc.internal.matchingParens.value
  def tokenList: TokenList =
    internal.doc.internal.tokenList.value
  def comments: AssociatedComments =
    internal.doc.internal.comments.value

  def diagnostics: Iterator[Diagnostic] =
    internal.messages
  def synthetics: Iterator[SemanticTree] =
    internal.synthetics

  override def info(symbol: Symbol): Option[SymbolInformation] =
    internal.info(symbol)
  override def toString: String =
    s"SemanticDocument(${input.syntax})"
}

object SemanticDocument {
  sealed abstract class Error(msg: String) extends Exception(msg)
  object Error {
    final case class MissingSemanticdb(reluri: String)
        extends Error(s"SemanticDB not found: $reluri")
    final case class MissingTextDocument(reluri: String)
        extends Error(s"TextDocument.uri not found: $reluri")
  }

  private[scalafix] def fromPath(
      doc: SyntacticDocument,
      path: RelativePath,
      classLoader: ClassLoader,
      symtab: SymbolTable
  ): SemanticDocument = {
    val semanticdbRelPath = s"META-INF/semanticdb/$path.semanticdb"
    Option(classLoader.getResourceAsStream(semanticdbRelPath)) match {
      case Some(inputStream) =>
        val sdocs =
          try s.TextDocuments.parseFrom(inputStream).documents
          finally inputStream.close()
        val reluri = path.toURI(isDirectory = false).toString
        val sdoc = sdocs.find(_.uri == reluri).getOrElse {
          throw Error.MissingTextDocument(reluri)
        }
        val impl = new InternalSemanticDoc(doc, sdoc, symtab)
        new SemanticDocument(impl)
      case None =>
        throw Error.MissingSemanticdb(semanticdbRelPath)
    }
  }
}
