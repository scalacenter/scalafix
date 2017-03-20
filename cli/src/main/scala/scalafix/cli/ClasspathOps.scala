package scalafix.cli

import java.io.File

object ClasspathOps {
  def getCurrentClasspath: String = {
    Thread.currentThread.getContextClassLoader match {
      case url: java.net.URLClassLoader =>
        url.getURLs.map(_.getFile).mkString(File.pathSeparator)
      case _ => ""
    }
  }
}
