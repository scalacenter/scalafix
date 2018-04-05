package scalafix.internal.util

import java.io.IOException
import scala.meta.internal.semanticdb3.Scala.Symbols
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Files
import org.langmeta.internal.io.PathIO
import org.langmeta.io.AbsolutePath
import org.langmeta.io.Classpath
import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb3.Scala._
import scala.meta.internal.{semanticdb3 => s}
import scala.util.control.NonFatal

/**
  * Implementation of SymbolTable that lazily loads symbols on demand.
  *
  * @param mclasspath The classpath to load symbols from. Needs to be pre-processed by metacp,
  *                   see ClasspathOps.toMclasspath in scalafix-cli.
  */
class LazySymbolTable(mclasspath: Classpath) extends SymbolTable {

  // Map from symbols to the paths to their corresponding semanticdb file where they are stored.
  private val notYetLoadedSymbols = TrieMap.empty[String, AbsolutePath]
  private val loadedSymbols = TrieMap.empty[String, s.SymbolInformation]
  private val semanticdb = "META-INF/semanticdb"
  private val semanticIdx = "META-INF/semanticdb.semanticidx"

  mclasspath.shallow.foreach(loadSemanticdbIndex)

  override def info(symbol: String): Option[s.SymbolInformation] = {
    var result = loadedSymbols.get(symbol)
    if (result.isEmpty) {
      loadSymbolFromClasspath(symbol)
      result = loadedSymbols.get(symbol)
      if (result.isEmpty && symbol.owner != Symbols.None) {
        info(symbol.owner)
        result = loadedSymbols.get(symbol)
      }
    }
    result
  }

  private def loadSymbolFromClasspath(symbol: String): Unit = {
    val toLoad = notYetLoadedSymbols.get(symbol)
    if (toLoad.isDefined) {
      val in = toLoad.get.toNIO.toUri.toURL.openStream()
      val semanticdb =
        try s.TextDocuments.parseFrom(in)
        finally in.close()
      semanticdb.documents.foreach { document =>
        document.symbols.foreach { info =>
          loadedSymbols.put(info.symbol, info)
        }
      }
      notYetLoadedSymbols.remove(symbol)
    }
  }

  private def loadIndex(root: AbsolutePath): Unit = {
    val path = root.resolve("META-INF").resolve("semanticdb.semanticidx")
    if (!Files.isRegularFile(path.toNIO)) return
    val in = Files.newInputStream(path.toNIO)
    val index =
      try s.Index.parseFrom(in)
      finally in.close()
    index.toplevels.foreach { toplevel =>
      notYetLoadedSymbols.put(
        toplevel.symbol,
        root.resolve(semanticdb).resolve(toplevel.uri))
    }
  }

  private def loadSemanticdbIndex(root: AbsolutePath): Unit = {
    if (root.isDirectory) {
      loadIndex(root)
    } else if (PathIO.extension(root.toNIO) == "jar") {
      withJarFileSystem(root)(loadIndex)
    } else {
      throw new IllegalArgumentException(root.toString())
    }
  }

  private def withJarFileSystem[T](path: AbsolutePath)(
      f: AbsolutePath => T): T = {
    val fs = newJarFileSystem(path)
    try {
      f(AbsolutePath(fs.getPath("/")))
    } catch {
      case NonFatal(e) =>
        throw new IOException(path.toString, e)
    }
  }

  private def newJarFileSystem(path: AbsolutePath): FileSystem = {
    Files.createDirectories(path.toNIO.getParent)
    val map = new java.util.HashMap[String, String]()
    val uri = URI.create("jar:file:" + path.toNIO.toUri.getPath)
    newFileSystem(uri, map)
  }

  private def newFileSystem(
      uri: URI,
      map: java.util.Map[String, _]): FileSystem =
    try FileSystems.newFileSystem(uri, map)
    catch {
      case _: FileSystemAlreadyExistsException =>
        FileSystems.getFileSystem(uri)
    }

}
