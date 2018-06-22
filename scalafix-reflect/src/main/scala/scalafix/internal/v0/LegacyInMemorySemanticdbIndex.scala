package scalafix.internal.v0

import scala.meta._
import scala.meta.internal.io._
import scala.meta.internal.{semanticdb => s}
import scala.{meta => m}
import scalafix.internal.patch.CrashingSemanticdbIndex
import scalafix.internal.patch.DocSemanticdbIndex
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.TreePos
import scalafix.util.SemanticdbIndex
import scalafix.v0.Database
import scalafix.v0.Denotation
import scalafix.v0.ResolvedName
import scalafix.v0
import scalafix.v1
import scalafix.internal.v1._

case class LegacyInMemorySemanticdbIndex(index: Map[String, SemanticdbIndex])
    extends CrashingSemanticdbIndex {

  override def inputs: Seq[m.Input] = {
    index.values.collect {
      case s: DocSemanticdbIndex =>
        s.doc.input
    }.toSeq
  }

  override def symbol(position: Position): Option[v0.Symbol] = {
    val key = position.input.syntax
    index(key).symbol(position) match {
      case Some(v0.Symbol.Local(id)) =>
        Some(v0.Symbol.Local(s"$key-$id"))
      case s => s
    }
  }
  override def symbol(tree: Tree): Option[v0.Symbol] =
    symbol(TreePos.symbol(tree))
  override def denotation(symbol: v0.Symbol): Option[Denotation] =
    symbol match {
      case v0.Symbol.Local(id) =>
        val dash = id.indexOf('-')
        if (dash >= 0) {
          val key = id.substring(0, dash)
          val local = id.substring(dash + 1)
          index(key).denotation(v0.Symbol.Local(local))
        } else {
          throw new IllegalArgumentException(
            s"unexpected local symbol format $id"
          )
        }
      case _ =>
        // global symbol, use any SemanticDoc
        index.head._2.denotation(symbol)
    }
  override def denotation(tree: Tree): Option[Denotation] =
    index(tree.pos.input.syntax).denotation(tree)

  override def database: Database =
    Database(documents)

  override def names: Seq[ResolvedName] =
    index.values.iterator.flatMap(_.names).toSeq
}

object LegacyInMemorySemanticdbIndex {

  def load(classpath: Classpath, sourceroot: AbsolutePath): SemanticdbIndex = {
    val symtab = ClasspathOps.newSymbolTable(classpath).get
    load(classpath, symtab, sourceroot)
  }

  def load(
      classpath: Classpath,
      symtab: SymbolTable,
      sourceroot: AbsolutePath
  ): SemanticdbIndex = {
    val sourceuri = sourceroot.toURI
    val buf = Map.newBuilder[String, SemanticdbIndex]
    classpath.entries.foreach { entry =>
      if (entry.isDirectory) {
        val files = FileIO.listAllFilesRecursively(
          entry.resolve("META-INF").resolve("semanticdb")
        )
        files.foreach { file =>
          if (PathIO.extension(file.toNIO) == "semanticdb") {
            val textDocument = s.TextDocuments
              .parseFrom(file.readAllBytes)
              .mergeDiagnosticOnlyDocuments
            textDocument.documents.foreach { textDocument =>
              val input = textDocument.input(sourceuri)
              val tree = input.parse[Source].get
              val doc = v1.Doc.fromTree(tree)
              val sdoc = new v1.SemanticDoc(doc, textDocument, symtab)
              buf += (textDocument.uri -> sdoc.toSemanticdbIndex)
            }
          }
        }
      }
    }
    LegacyInMemorySemanticdbIndex(buf.result())
  }

}
