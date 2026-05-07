package scalafix.internal.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.net.URL

import scala.meta.io.AbsolutePath

object FileOps {

  def listFiles(path: String): Vector[String] = {
    listFiles(new File(path))
  }

  def listFiles(file: File): Vector[String] = {
    if (file.isFile) {
      Vector(file.getAbsolutePath)
    } else {
      val res = Vector.newBuilder[String]
      def listFilesIter(s: File): Unit = {
        val allFiles = s.listFiles()
        if (allFiles ne null) {
          allFiles.foreach { f =>
            if (f.isDirectory) listFilesIter(f)
            else res += f.getPath
          }
        }
      }
      listFilesIter(file)
      res.result()
    }
  }

  def readURL(url: URL): String = {
    val src = scala.io.Source.fromURL(url)("UTF-8")
    try src.getLines().mkString("\n")
    finally src.close()
  }

  /**
   * Reads file from file system or from http url.
   */
  def readFile(filename: String): String = {
    if (filename matches "https?://.*") {
      readURL(new URL(filename))
    } else {
      readFile(new File(filename))
    }
  }

  def readFile(file: File): String = {
    // Prefer this to inefficient Source.fromFile.
    val sb = new StringBuilder
    val br = new BufferedReader(new FileReader(file))
    val lineSeparator = System.getProperty("line.separator")
    try {
      var line = ""
      while ({
        line = br.readLine()
        line != null
      }) {
        sb.append(line)
        sb.append(lineSeparator)
      }
    } finally {
      br.close()
    }
    sb.toString()
  }

  def getFile(path: String*): File = {
    new File(path.mkString(File.separator))
  }

  def writeFile(file: AbsolutePath, content: String): Unit = {
    writeFile(file.toString(), content)
  }

  def writeFile(file: File, content: String): Unit = {
    writeFile(file.getAbsolutePath, content)
  }

  def writeFile(filename: String, content: String): Unit = {
    // For java 6 compatibility we don't use java.nio.
    val pw = new PrintWriter(new File(filename))
    try {
      pw.write(content)
    } finally {
      pw.close()
    }
  }
}
