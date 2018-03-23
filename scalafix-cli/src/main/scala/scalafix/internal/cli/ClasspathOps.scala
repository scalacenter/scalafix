package scalafix.internal.cli

import java.io.File
import java.io.PrintStream
import scala.meta.Classpath
import scala.meta.metacp
import scalafix.internal.util.LazySymbolTable
import scalafix.internal.util.SymbolTable

object ClasspathOps {

  def bootClasspath: Option[Classpath] = sys.props.collectFirst {
    case (k, v) if k.endsWith(".boot.class.path") => Classpath(v)
  }

  /**
    * Process classpath with metacp to build semanticdbs of global symbols.
    *
    * @param sclasspath Regular classpath to process.
    * @param out The output stream to print out error messages.
    */
  def toMetaClasspath(sclasspath: Classpath, out: PrintStream): Classpath = {
    val withJDK = Classpath(
      bootClasspath.fold(sclasspath.shallow)(_.shallow ::: sclasspath.shallow))
    val settings = metacp
      .Settings()
      .withClasspath(withJDK)
      .withScalaLibrarySynthetics(true)
    val reporter = metacp.Reporter().withOut(out)
    val mclasspath = scala.meta.cli.Metacp.process(settings, reporter).get
    mclasspath
  }

  def newSymbolTable(classpath: Classpath, out: PrintStream): SymbolTable = {
    val mclasspath = toMetaClasspath(classpath, out)
    new LazySymbolTable(mclasspath)
  }

  def getCurrentClasspath: String = {
    Thread.currentThread.getContextClassLoader match {
      case url: java.net.URLClassLoader =>
        url.getURLs.map(_.getFile).mkString(File.pathSeparator)
      case _ => ""
    }
  }
}
