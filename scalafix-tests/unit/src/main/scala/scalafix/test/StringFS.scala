package scalafix.test

import java.io.File
import java.nio.file.Files
import scala.meta.AbsolutePath

object StringFS {

  /**
    * The inverse of [[dir2string]]. Given a string representation creates the
    * necessary files/directories with respective file contents.
    */
  def string2dir(layout: String): AbsolutePath = {
    val root = Files.createTempDirectory("root")
    layout.split("(?=\n/)").foreach { row =>
      val path :: contents :: Nil =
        row.stripPrefix("\n").split("\n", 2).toList
      val file = root.resolve(path)
      file.getParent.toFile.mkdirs()
      Files.write(file, contents.getBytes)
    }
    AbsolutePath(root)
  }

  /** Gives a string representation of a directory. For example
    *
    * /build.sbt
    * val x = project
    * /src/main/scala/Main.scala
    * object A { def main = Unit }
    * /target/scala-2.11/foo.class
    * ^!*@#@!*#&@*!&#^
    */
  def dir2string(file: AbsolutePath): String = {
    import scala.collection.JavaConverters._
    Files
      .list(file.toNIO)
      .iterator()
      .asScala
      .toArray
      .sorted
      .map { path =>
        val contents = new String(Files.readAllBytes(path))
        s"""|${file.toNIO.relativize(path)}
            |$contents""".stripMargin
      }
      .mkString("\n")
      .replace(File.separator, "/") // ensure original separators
  }

}
