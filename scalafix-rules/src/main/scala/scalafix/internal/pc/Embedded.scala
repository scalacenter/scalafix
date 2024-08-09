package scalafix.internal.pc

import java.net.URLClassLoader
import java.util.ServiceLoader

import scala.jdk.CollectionConverters._

import scala.meta.pc.PresentationCompiler

import coursierapi.Dependency
import coursierapi.Fetch
import coursierapi.MavenRepository
import scala.collection.concurrent.TrieMap

object Embedded {

  private val presentationCompilers: TrieMap[String, URLClassLoader] =
    TrieMap.empty

  def presentationCompiler(
      scalaVersion: String
  ): PresentationCompiler = {
    val classloader = presentationCompilers.getOrElseUpdate(
      scalaVersion,
      newPresentationCompilerClassLoader(scalaVersion)
    )
    val presentationCompilerClassname =
      if (supportPresentationCompilerInDotty(scalaVersion)) {
        "dotty.tools.pc.ScalaPresentationCompiler"
      } else {
        "scala.meta.pc.ScalaPresentationCompiler"
      }

    serviceLoader(
      classOf[PresentationCompiler],
      presentationCompilerClassname,
      classloader
    )
  }

  private def supportPresentationCompilerInDotty(scalaVersion: String) = {
    scalaVersion
      .replaceAll(raw"-RC\d+", "")
      .split("\\.")
      .take(3)
      .map(_.toInt) match {
      case Array(3, minor, patch) => minor > 3 || minor == 3 && patch >= 4
      case _ => false
    }
  }

  private def scala3PresentationCompilerDependencies(version: String) =
    if (supportPresentationCompilerInDotty(version)) {
      val dep = Dependency
        .of("org.scala-lang", "scala3-presentation-compiler_3", version)

      // some versions of the presentation compiler depend on versions only build for JDK 11
      dep.addExclusion("org.eclipse.lsp4j", "org.eclipse.lsp4j")
      dep.addExclusion("org.eclipse.lsp4j", "org.eclipse.lsp4j.jsonrpc")
      // last built with JDK 8
      val lsp4jDep =
        Dependency.of("org.eclipse.lsp4j", "org.eclipse.lsp4j", "0.20.1")
      List(dep, lsp4jDep)
    } else
      List(
        // TODO should use build info etc. instead of using 1.3.4
        Dependency.of("org.scalameta", s"mtags_${version}", "1.3.4")
      )

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
      scala3PresentationCompilerDependencies(scalaVersion)
    val jars = Fetch
      .create()
      .addDependencies(deps: _*)
      .addRepositories(
        MavenRepository.of(
          "https://oss.sonatype.org/content/repositories/snapshots"
        )
      )
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
