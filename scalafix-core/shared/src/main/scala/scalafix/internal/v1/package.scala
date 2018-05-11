package scalafix.internal

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.langmeta.io.AbsolutePath
import org.langmeta.io.Classpath
import org.langmeta.io.RelativePath
import scala.collection.JavaConverters._

package object v1 {

  implicit class XtensionRelativePathScalafix(path: RelativePath) {
    // TODO: replace with RelativePath.toURI once https://github.com/scalameta/scalameta/issues/1523 is fixed
    def toRelativeURI: URI = {
      val reluri = path.toNIO.asScala.iterator.map { path =>
        URLEncoder.encode(
          path.getFileName.toString,
          StandardCharsets.UTF_8.name())
      }
      URI.create(reluri.mkString("/"))
    }
  }

  implicit class XtensionClasspathScalafix(cp: Classpath) {
    def resolveSemanticdb(path: RelativePath): Option[AbsolutePath] = {
      cp.shallow.iterator
        .filter(_.isDirectory)
        .map(
          _.resolve("META-INF")
            .resolve("semanticdb")
            .resolve(path.toString() + ".semanticdb"))
        .collectFirst { case p if p.isFile => p }
    }
  }

}
