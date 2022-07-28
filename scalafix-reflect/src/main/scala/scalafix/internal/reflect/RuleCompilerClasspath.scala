package scalafix.internal.reflect

import java.io.File
import java.nio.file.Paths

import scala.meta.io.AbsolutePath

object RuleCompilerClasspath {

  def defaultClasspath: String = {
    defaultClasspathPaths.mkString(File.pathSeparator)
  }

  def defaultClasspathPaths: List[AbsolutePath] = {
    val classLoader = ClasspathOps.thisClassLoader
    val paths = classLoader.getURLs.iterator.map { u =>
      if (u.getProtocol.startsWith("bootstrap")) {
        import java.nio.file._
        val stream = u.openStream
        val tmp = Files.createTempFile("bootstrap-" + u.getPath, ".jar")
        try {
          Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          stream.close()
        }
        AbsolutePath(tmp)
      } else {
        AbsolutePath(Paths.get(u.toURI))
      }
    }
    paths.toList
  }
}
