package scalafix.internal.tests.utils

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.collection.JavaConverters._

class Fs() {
  val workingDirectory: Path =
    Files.createTempDirectory("scalafix")

  workingDirectory.toFile.deleteOnExit()

  def mkdir(dirname: String): Unit =
    Files.createDirectory(path(dirname))

  def add(filename: String, content: String): Unit =
    write(filename, content, StandardOpenOption.CREATE_NEW)

  def append(filename: String, content: String): Unit =
    write(filename, content, StandardOpenOption.APPEND)

  def replace(filename: String, content: String): Unit = {
    rm(filename)
    add(filename, content)
  }

  def read(src: String): String =
    Files.readAllLines(path(src)).asScala.mkString("\n")

  def rm(filename: String): Unit =
    Files.delete(path(filename))

  def mv(src: String, dst: String): Unit =
    Files.move(path(src), path(dst))

  def absPath(filename: String): String =
    path(filename).toAbsolutePath.toString

  private def write(
      filename: String,
      content: String,
      op: StandardOpenOption): Unit = {
    Files.write(path(filename), content.getBytes, op)
  }

  private def path(filename: String): Path =
    workingDirectory.resolve(filename)
}
