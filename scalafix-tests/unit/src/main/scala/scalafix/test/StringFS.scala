package scalafix.test

import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import scala.meta.AbsolutePath

object StringFS {

  /**
    * The inverse of [[dir2string]]. Given a string representation creates the
    * necessary files/directories with respective file contents.
    */
  def string2dir(
      layout: String,
      charset: Charset = StandardCharsets.UTF_8): AbsolutePath = {
    val root = Files.createTempDirectory("root")
    if (!layout.isEmpty) {
      layout.split("(?=\n/)").foreach { row =>
        row.stripPrefix("\n").split("\n", 2).toList match {
          case path :: contents :: Nil =>
            val file = root.resolve(path.stripPrefix("/"))
            file.getParent.toFile.mkdirs()
            Files.write(file, contents.getBytes(charset))
          case els =>
            throw new IllegalArgumentException(
              s"Unable to split argument info path/contents! \n$els")

        }
      }
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
  def dir2string(
      file: AbsolutePath,
      charset: Charset = StandardCharsets.UTF_8): String = {
    import scala.collection.JavaConverters._
    Files
      .walk(file.toNIO)
      .iterator()
      .asScala
      .filter(_.toFile.isFile)
      .toArray
      .sorted
      .map { path =>
        val contents = new String(Files.readAllBytes(path), charset)
        s"""|/${file.toNIO.relativize(path)}
            |$contents""".stripMargin
      }
      .mkString("\n")
      .replace(File.separator, "/") // ensure original separators
  }

}
