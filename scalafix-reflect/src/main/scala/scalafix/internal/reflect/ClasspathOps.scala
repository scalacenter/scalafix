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
import scala.meta.io.AbsolutePath
import scala.meta.Classpath
import scala.meta.metacp
import scala.meta.internal.symtab._

object ClasspathOps {

  def bootClasspath: Option[Classpath] = sys.props.collectFirst {
    case (k, v) if k.endsWith(".boot.class.path") => Classpath(v)
  }

  val devNull = new PrintStream(new OutputStream {
    override def write(b: Int): Unit = ()
  })

  /** Process classpath with metacp to build semanticdbs of global symbols. **/
  def toMetaClasspath(
      sclasspath: Classpath,
      cacheDirectory: Option[AbsolutePath] = None,
      parallel: Boolean = false,
      out: PrintStream = devNull
  ): Option[Classpath] = {
    val (processed, toProcess) = sclasspath.entries.partition { path =>
      path.isDirectory &&
      path.resolve("META-INF").resolve("semanticdb.semanticidx").isFile
    }
    val withJDK = Classpath(
      bootClasspath.fold(sclasspath.entries)(_.entries ::: toProcess)
    )
    val default = metacp.Settings()
    val settings = default
      .withClasspath(withJDK)
      .withScalaLibrarySynthetics(true)
      .withPar(parallel)
    val reporter = scala.meta.cli
      .Reporter()
      .withOut(devNull) // out prints classpath of proccessed classpath, which is not relevant for scalafix.
      .withErr(out)
    val mclasspath = scala.meta.cli.Metacp.process(settings, reporter)
    mclasspath.map(x => Classpath(x.entries ++ processed))
  }

  def newSymbolTable(
      classpath: Classpath,
      cacheDirectory: Option[AbsolutePath] = None,
      parallel: Boolean = false,
      out: PrintStream = System.out
  ): Option[SymbolTable] = {
    bootClasspath.map { jdk =>
      GlobalSymbolTable(classpath ++ jdk)
    }
  }

  def getCurrentClasspath: String = {
    Thread.currentThread.getContextClassLoader match {
      case url: java.net.URLClassLoader =>
        url.getURLs.map(_.getFile).mkString(File.pathSeparator)
      case _ => ""
    }
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

  def toClassLoader(classpath: Classpath): ClassLoader = {
    val urls = classpath.entries.map(_.toNIO.toUri.toURL).toArray
    new URLClassLoader(urls, null)
  }
}
