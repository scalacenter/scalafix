package scalafix.internal.v0

import scala.meta._
import scala.meta.internal.io._
import scala.meta.internal.{semanticdb3 => s}
import scala.{meta => m}
import scalafix.internal.patch.CrashingSemanticdbIndex
import scalafix.internal.patch.DeprecatedSemanticdbIndex
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.TreePos
import scalafix.util.SemanticdbIndex
import scalafix.v0.Denotation
import scalafix.v1

case class LegacyInMemorySemanticdbIndex(index: Map[String, SemanticdbIndex])
    extends CrashingSemanticdbIndex {

  override def inputs: Seq[m.Input] = {
    index.values.collect {
      case s: DeprecatedSemanticdbIndex =>
        s.doc.input
    }.toSeq
  }

  override def symbol(position: Position): Option[Symbol] = {
    val key = position.input.syntax
    index(key).symbol(position) match {
      case Some(m.Symbol.Local(id)) =>
        Some(m.Symbol.Local(s"$key-$id"))
      case s => s
    }
  }
  override def symbol(tree: Tree): Option[Symbol] =
    symbol(TreePos.symbol(tree))
  override def denotation(symbol: Symbol): Option[Denotation] = symbol match {
    case m.Symbol.Local(id) =>
      val dash = id.indexOf('-')
      if (dash >= 0) {
        val key = id.substring(0, dash)
        val local = id.substring(dash + 1)
        index(key).denotation(m.Symbol.Local(local))
      } else {
        throw new IllegalArgumentException(
          s"unexpected local symbol format $id")
      }
    case _ =>
      // global symbol, use any SemanticDoc
      index.head._2.denotation(symbol)
  }
  override def denotation(tree: Tree): Option[Denotation] =
    index(tree.pos.input.syntax).denotation(tree)
}

object LegacyInMemorySemanticdbIndex {

  def load(classpath: Classpath): SemanticdbIndex = {
    val symtab = ClasspathOps.newSymbolTable(classpath).get
    load(classpath, symtab)
  }

  def load(classpath: Classpath, symtab: SymbolTable): SemanticdbIndex = {
    val buf = Map.newBuilder[String, SemanticdbIndex]
    classpath.entries.foreach { entry =>
      if (entry.isDirectory) {
        val files = FileIO.listAllFilesRecursively(
          entry.resolve("META-INF").resolve("semanticdb"))
        files.foreach { file =>
          if (PathIO.extension(file.toNIO) == "semanticdb") {
            val textDocument = s.TextDocuments.parseFrom(file.readAllBytes)
            textDocument.documents.foreach { textDocument =>
              val input = Input.VirtualFile(textDocument.uri, textDocument.text)
              val tree = input.parse[Source].get
              val doc = v1.Doc.fromTree(tree)
              val sdoc = new v1.SemanticDoc(doc, textDocument, symtab)
              buf += (textDocument.uri -> sdoc.toLegacy)
            }
          }
        }
      }
    }
    LegacyInMemorySemanticdbIndex(buf.result())
  }

}
