package scalafix.tests.core

import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.reflect.RuleCompiler

object Classpaths {
  def jdk: Classpath = ClasspathOps.bootClasspath.get
  def scalaLibrary = Classpath(
    RuleCompiler.defaultClasspathPaths.filter { path =>
      path.isFile ||
      path.toNIO.getFileName.toString.contains("scala-library")
    }
  )
  def withDirectory(dir: AbsolutePath) =
    Classpath(dir :: jdk.entries ::: scalaLibrary.entries)

}
