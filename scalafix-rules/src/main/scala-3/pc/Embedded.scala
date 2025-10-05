package scalafix.internal.pc

import java.net.URLClassLoader
import java.util.ServiceLoader

import scala.jdk.CollectionConverters.*

import scala.meta.pc.PresentationCompiler

import coursierapi.Dependency
import coursierapi.Fetch

object Embedded {

  private val presentationCompilers =
    new java.util.concurrent.ConcurrentHashMap[String, URLClassLoader]()

  def presentationCompiler(
      scalaVersion: String
  ): PresentationCompiler = {
    val classloader = presentationCompilers.computeIfAbsent(
      scalaVersion,
      newPresentationCompilerClassLoader
    )

    serviceLoader(
      classOf[PresentationCompiler],
      "dotty.tools.pc.ScalaPresentationCompiler",
      classloader
    )
  }

  private def serviceLoader[T](
      cls: Class[T],
      className: String,
      classloader: URLClassLoader
  ): T = {
    val services = ServiceLoader.load(cls, classloader).iterator()
    if (services.hasNext) services.next()
    else {
      val cls = classloader.loadClass(className)
      val ctor = cls.getDeclaredConstructor()
      ctor.setAccessible(true)
      ctor.newInstance().asInstanceOf[T]
    }
  }

  private def newPresentationCompilerClassLoader(
      scalaVersion: String
  ): URLClassLoader = {

    val deps =
      List(
        Dependency.of(
          "org.scala-lang",
          "scala3-presentation-compiler_3",
          scalaVersion
        )
      )
    val jars = Fetch
      .create()
      .addDependencies(deps: _*)
      .fetch()
      .asScala
      .map(_.toPath())
      .toSeq
    val allJars = jars.iterator
    val allURLs = allJars.map(_.toUri.toURL).toArray
    // Share classloader for a subset of types.
    val parent =
      new scalafix.internal.pc.PresentationCompilerClassLoader(
        this.getClass.getClassLoader
      )
    new URLClassLoader(allURLs, parent)
  }
}
