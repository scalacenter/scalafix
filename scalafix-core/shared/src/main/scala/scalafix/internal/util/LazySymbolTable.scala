package scalafix.internal.util

import scala.meta.internal.semanticdb3.Scala.Symbols
import java.io.InputStream
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

/**
  * Implementation of SymbolTable that lazily loads symbols on demand.
  *
  * @param mclasspath The classpath to load symbols from. Needs to be pre-processed by metacp,
  *                   see ClasspathOps.toMclasspath in scalafix-cli.
  */
class LazySymbolTable(mclasspath: Classpath) extends SymbolTable {

  // Map from symbols to the paths to their corresponding semanticdb file where they are stored.
  private val unloadedSymbols = TrieMap.empty[String, AbsolutePath]
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
    val toLoad = unloadedSymbols.get(symbol)
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
      unloadedSymbols.remove(symbol)
    }
  }

  private def loadIndex(root: AbsolutePath, in: InputStream): Unit = {
    val index =
      try s.Index.parseFrom(in)
      finally in.close()
    index.toplevels.foreach { toplevel =>
      unloadedSymbols.put(
        toplevel.symbol,
        root.resolve(semanticdb).resolve(toplevel.uri))
    }
  }

  private def loadSemanticdbIndex(root: AbsolutePath): Unit = {
    if (root.isDirectory) {
      loadIndex(root, Files.newInputStream(root.resolve(semanticIdx).toNIO))
    } else if (PathIO.extension(root.toNIO) == "jar") {
      withJarFileSystem(root) { jarRoot =>
        loadIndex(
          jarRoot,
          Files.newInputStream(jarRoot.resolve(semanticIdx).toNIO)
        )
      }
    } else {
      throw new IllegalArgumentException(root.toString())
    }
  }

  private def withJarFileSystem[T](path: AbsolutePath)(
      f: AbsolutePath => T): T = {
    val fs = newJarFileSystem(path)
    // NOTE(olafur): We don't fs.close() because that can affect another place where `FileSystems.getFileSystems`
    // was used due to a `FileSystemAlreadyExistsException`. I don't know what the best solution is for reading the
    // same zip file from multiple concurrent places.
    f(AbsolutePath(fs.getPath("/")))
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
