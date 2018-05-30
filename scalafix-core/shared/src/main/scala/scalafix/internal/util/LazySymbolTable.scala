package scalafix.internal.util

import java.nio.file.Files
import java.util.jar.JarFile
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.collection.concurrent.TrieMap
import scala.meta.internal.semanticdb3.Scala.Symbols
import scala.meta.internal.semanticdb3.Scala._
import scala.meta.internal.{semanticdb3 => s}

/**
  * Implementation of SymbolTable that lazily loads symbols on demand.
  *
  * @note Credits to the rsc symbol table implementation
  *       https://github.com/twitter/rsc/blob/c82ce2ee68729e2cbcca7c96a164a303a152a43c/rsc/src/main/scala/rsc/symtab/Loaders.scala
  *       that was a source of inspiration for the implementation of this class.
  * @param mclasspath The classpath to load symbols from. Needs to be pre-processed by metacp,
  *                   see ClasspathOps.toMclasspath in scalafix-cli.
  */
class LazySymbolTable(mclasspath: Classpath) extends SymbolTable {

  // Map from symbols to the paths to their corresponding semanticdb file where they are stored.
  private val notYetLoadedSymbols = TrieMap.empty[String, SemanticdbEntry]
  private val loadedSymbols = TrieMap.empty[String, s.SymbolInformation]

  mclasspath.entries.foreach(loadSemanticdbIndex)

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
      val in = toLoad.get match {
        case Uncompressed(path, uri) =>
          Files.newInputStream(
            path.resolve("META-INF").resolve("semanticdb").resolve(uri).toNIO)
        case Compressed(jar, entry) =>
          jar.getInputStream(jar.getEntry("META-INF/semanticdb/" + entry))
      }
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

  private def loadSemanticdbIndex(root: AbsolutePath): Unit = {
    if (root.isDirectory) {
      val indexPath = root.resolve("META-INF").resolve("semanticdb.semanticidx")
      if (indexPath.isFile) {
        val in = Files.newInputStream(indexPath.toNIO)
        val index =
          try s.Index.parseFrom(in)
          finally in.close()
        indexPackages(index)
        index.toplevels.foreach { toplevel =>
          notYetLoadedSymbols.put(
            toplevel.symbol,
            Uncompressed(root, toplevel.uri))
        }
      }
    } else if (PathIO.extension(root.toNIO) == "jar") {
      val jar = new JarFile(root.toFile)
      val entry = jar.getEntry("META-INF/semanticdb.semanticidx")
      if (entry != null) {
        val in = jar.getInputStream(entry)
        val index =
          try s.Index.parseFrom(in)
          finally in.close()
        indexPackages(index)
        index.toplevels.foreach { toplevel =>
          notYetLoadedSymbols.put(
            toplevel.symbol,
            Compressed(jar, toplevel.uri)
          )
        }
      }
    } else {
      throw new IllegalArgumentException(root.toString())
    }
  }

  private def indexPackages(index: s.Index): Unit = {
    index.packages.foreach { pkg =>
      loadedSymbols(pkg.symbol) = s.SymbolInformation(
        name = pkg.symbol.desc.name,
        symbol = pkg.symbol,
        owner = pkg.symbol.owner,
        kind = s.SymbolInformation.Kind.PACKAGE
      )
    }
  }

  private sealed trait SemanticdbEntry
  private case class Uncompressed(root: AbsolutePath, uri: String)
      extends SemanticdbEntry
  private case class Compressed(jarFile: JarFile, entry: String)
      extends SemanticdbEntry

}
