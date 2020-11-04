package scalafix.internal.reflect

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util

import scala.meta.Classpath
import scala.meta.internal.symtab._
import scala.meta.io.AbsolutePath

import sun.misc.Unsafe

object ClasspathOps {

  val devNull = new PrintStream(new OutputStream {
    override def write(b: Int): Unit = ()
  })

  def newSymbolTable(
      classpath: Classpath,
      parallel: Boolean = false,
      out: PrintStream = System.out
  ): SymbolTable = {
    GlobalSymbolTable(classpath, includeJdk = true)
  }

  def thisClassLoader: URLClassLoader = {
    val classLoader = this.getClass.getClassLoader
    new URLClassLoader(getURLs(classLoader), classLoader)
  }

  def thisClassLoaderWith(url: URL): URLClassLoader = {
    val classLoader = this.getClass.getClassLoader
    new URLClassLoader(url +: getURLs(classLoader), classLoader)
  }

  def thisClasspath: Classpath = {
    Classpath(
      getURLs(this.getClass().getClassLoader())
        .map(url => AbsolutePath(Paths.get(url.toURI)))
        .toList
    )
  }

  def getCurrentClasspath: String = {
    getURLs(this.getClass.getClassLoader)
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

  private def isJar(path: AbsolutePath): Boolean =
    path.isFile &&
      path.toFile.getName.endsWith(".jar")

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
    roots.filter(isJar).foreach(buffer += _)
    Classpath(buffer.result())
  }

  def toClassLoader(classpath: Classpath): URLClassLoader = {
    toClassLoaderWithParent(classpath, this.getClass.getClassLoader)
  }
  def toOrphanClassLoader(classpath: Classpath): URLClassLoader = {
    toClassLoaderWithParent(classpath, null)
  }
  private def toClassLoaderWithParent(
      classpath: Classpath,
      parent: ClassLoader
  ): URLClassLoader = {
    val urls = classpath.entries.map(_.toNIO.toUri.toURL).toArray
    new URLClassLoader(urls, parent)
  }

  /**
   * Utility to get SystemClassLoader/ClassLoader urls in java8 and java9+
   *   Based upon: https://gist.github.com/hengyunabc/644f8e84908b7b405c532a51d8e34ba9
   */
  def getURLs(classLoader: ClassLoader): Array[URL] = {
    if (classLoader.isInstanceOf[URLClassLoader]) {
      classLoader.asInstanceOf[URLClassLoader].getURLs()
      // java9+
    } else if (
      classLoader
        .getClass()
        .getName()
        .startsWith("jdk.internal.loader.ClassLoaders$")
    ) {
      try {
        val field = classOf[Unsafe].getDeclaredField("theUnsafe")
        field.setAccessible(true)
        val unsafe = field.get(null).asInstanceOf[Unsafe]

        // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        val ucpField = classLoader.getClass().getDeclaredField("ucp")
        val ucpFieldOffset: Long = unsafe.objectFieldOffset(ucpField)
        val ucpObject = unsafe.getObject(classLoader, ucpFieldOffset)

        // jdk.internal.loader.URLClassPath.path
        val pathField = ucpField.getType().getDeclaredField("path")
        val pathFieldOffset = unsafe.objectFieldOffset(pathField)
        val paths = unsafe
          .getObject(ucpObject, pathFieldOffset)
          .asInstanceOf[util.ArrayList[URL]]

        paths.toArray(new Array[URL](paths.size))
      } catch {
        case ex: Exception =>
          ex.printStackTrace()
          Array.empty
      }
    } else {
      Array.empty
    }
  }
}
