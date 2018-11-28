package scalafix.tests.core

import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scalafix.internal.reflect.RuleCompiler

object Classpaths {
  def scalaLibrary = Classpath(
    RuleCompiler.defaultClasspathPaths.filter { path =>
      path.isFile ||
      path.toNIO.getFileName.toString.contains("scala-library")
    }
  )
  def withDirectory(dir: AbsolutePath): Classpath =
    Classpath(dir :: scalaLibrary.entries)

}
