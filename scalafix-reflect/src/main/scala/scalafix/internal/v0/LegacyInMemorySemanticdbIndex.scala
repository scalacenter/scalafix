package scalafix.internal.v0

import scala.meta._
import scala.meta.internal.io._
import scala.meta.internal.{semanticdb => s}
import scala.{meta => m}
import scalafix.internal.patch.CrashingSemanticdbIndex
import scalafix.internal.reflect.ClasspathOps
import scala.meta.internal.symtab.SymbolTable
import scalafix.internal.v1.TreePos
import scalafix.util.SemanticdbIndex
import scalafix.v0.Database
import scalafix.v0.Denotation
import scalafix.v0.ResolvedName
import scalafix.v0.Synthetic
import scalafix.v0
import scalafix.v1
import scalafix.internal.v1._

case class LegacyInMemorySemanticdbIndex(
    index: Map[String, SemanticdbIndex],
    symtab: SymbolTable)
    extends CrashingSemanticdbIndex
    with SymbolTable {

  def info(symbol: String): Option[s.SymbolInformation] = symtab.info(symbol)

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

  def synthetics(input: Input): Seq[Synthetic] =
    index(input.syntax).synthetics

  override def documents: Seq[v0.Document] =
    throw new UnsupportedOperationException()

  override def database: Database = throw new UnsupportedOperationException()

  override def names: Seq[ResolvedName] =
    index.values.iterator.flatMap(_.names).toSeq
}

object LegacyInMemorySemanticdbIndex {

  def load(
      classpath: Classpath,
      sourceroot: AbsolutePath): LegacyInMemorySemanticdbIndex = {
    val symtab = ClasspathOps.newSymbolTable(classpath).get
    val dialect = dialects.Scala212
    load(classpath, sourceroot, symtab, dialect)
  }

  def load(
      classpath: Classpath,
      sourceroot: AbsolutePath,
      symtab: SymbolTable,
      dialect: Dialect
  ): LegacyInMemorySemanticdbIndex = {
    val sourceuri = sourceroot.toURI
    val buf = Map.newBuilder[String, SemanticdbIndex]
    classpath.entries.foreach { entry =>
      if (entry.isDirectory) {
        val files = FileIO.listAllFilesRecursively(
          entry.resolve("META-INF").resolve("semanticdb")
        )
        files.foreach { file =>
          if (PathIO.extension(file.toNIO) == "semanticdb") {
            val textDocument = s.TextDocuments.parseFromFile(file)
            textDocument.documents.foreach { textDocument =>
              val input = textDocument.input(sourceuri)
              val doc = v1.Doc.fromInput(input, dialect)
              val internal = new InternalSemanticDoc(doc, textDocument, symtab)
              val sdoc = new v1.SemanticDoc(internal)
              buf += (textDocument.uri -> new DocSemanticdbIndex(sdoc))
            }
          }
        }
      }
    }
    LegacyInMemorySemanticdbIndex(buf.result(), symtab)
  }

}
