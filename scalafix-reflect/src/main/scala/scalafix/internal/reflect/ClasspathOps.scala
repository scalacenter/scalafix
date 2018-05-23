package scalafix.internal.reflect

import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import org.langmeta.io.AbsolutePath
import scala.meta.Classpath
import scala.meta.metacp
import scalafix.internal.util.LazySymbolTable
import scalafix.internal.util.SymbolTable

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
      parallel: Boolean = true,
      out: PrintStream = devNull): Option[Classpath] = {
    val (processed, toProcess) = sclasspath.shallow.partition { path =>
      path.isDirectory &&
      path.resolve("META-INF").resolve("semanticdb.semanticidx").isFile
    }
    val withJDK = Classpath(
      bootClasspath.fold(sclasspath.shallow)(_.shallow ::: toProcess))
    val default = metacp.Settings()
    val settings = default
      .withClasspath(withJDK)
      .withScalaLibrarySynthetics(true)
      .withCacheDir(cacheDirectory.getOrElse(default.cacheDir))
      .withPar(parallel)
    val reporter = metacp
      .Reporter()
      .withOut(devNull) // out prints classpath of proccessed classpath, which is not relevant for scalafix.
      .withErr(out)
    val mclasspath = scala.meta.cli.Metacp.process(settings, reporter)
    mclasspath.map(x => Classpath(x.shallow ++ processed))
  }

  def newSymbolTable(
      classpath: Classpath,
      cacheDirectory: Option[AbsolutePath] = None,
      parallel: Boolean = true,
      out: PrintStream = System.out): Option[SymbolTable] = {
    toMetaClasspath(classpath, cacheDirectory, parallel, out)
      .map(new LazySymbolTable(_))
  }

  def getCurrentClasspath: String = {
    Thread.currentThread.getContextClassLoader match {
      case url: java.net.URLClassLoader =>
        url.getURLs.map(_.getFile).mkString(File.pathSeparator)
      case _ => ""
    }
  }
}
