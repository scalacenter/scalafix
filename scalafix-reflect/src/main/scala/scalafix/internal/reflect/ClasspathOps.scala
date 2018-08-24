package scalafix.internal.reflect

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.meta.Classpath
import scala.meta.internal.symtab._
import scala.meta.io.AbsolutePath

object ClasspathOps {

  def bootClasspath: Option[Classpath] = sys.props.collectFirst {
    case (k, v) if k.endsWith(".boot.class.path") => Classpath(v)
  }

  val devNull = new PrintStream(new OutputStream {
    override def write(b: Int): Unit = ()
  })

  def newSymbolTable(
      classpath: Classpath,
      parallel: Boolean = false,
      out: PrintStream = System.out
  ): Option[SymbolTable] = {
    bootClasspath.map { jdk =>
      GlobalSymbolTable(classpath ++ jdk)
    }
  }

  def thisClassLoader: URLClassLoader =
    this.getClass.getClassLoader.asInstanceOf[URLClassLoader]
  def thisClasspath: Classpath = {
    Classpath(
      thisClassLoader.getURLs.iterator
        .map(url => AbsolutePath(Paths.get(url.toURI)))
        .toList
    )
  }
  this.getClass.getClassLoader.asInstanceOf[URLClassLoader]
  def getCurrentClasspath: String = {
    this.getClass.getClassLoader
      .asInstanceOf[URLClassLoader]
      .getURLs
      .map(_.getFile)
      .mkString(File.pathSeparator)
  }

  private val META_INF = Paths.get("META-INF")
  private val SEMANTICDB = Paths.get("semanticdb")

  private def isTargetroot(path: Path): Boolean = {
    path.toFile.isDirectory &&
    path.resolve(META_INF).toFile.isDirectory &&
    path.resolve(META_INF).resolve(SEMANTICDB).toFile.isDirectory
  }

  def autoClasspath(roots: List[AbsolutePath]): Classpath = {
    val buffer = List.newBuilder[AbsolutePath]
    val visitor = new SimpleFileVisitor[Path] {
      override def preVisitDirectory(
          dir: Path,
          attrs: BasicFileAttributes
      ): FileVisitResult = {
        if (isTargetroot(dir)) {
          buffer += AbsolutePath(dir)
          FileVisitResult.SKIP_SUBTREE
        } else {
          FileVisitResult.CONTINUE
        }
      }
    }
    roots.foreach(x => Files.walkFileTree(x.toNIO, visitor))
    Classpath(buffer.result())
  }

  def toClassLoader(classpath: Classpath): URLClassLoader = {
    val urls = classpath.entries.map(_.toNIO.toUri.toURL).toArray
    new URLClassLoader(urls, this.getClass.getClassLoader)
  }
}
