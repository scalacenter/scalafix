package scalafix.internal.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.net.URI
import java.net.URL
import java.net.URLConnection

import scala.meta.io.AbsolutePath

object FileOps {
  private val GitHubApiVersion = "2026-03-10"

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

  def readURL(url: URL): String =
    readURL(url, _.openConnection())

  private[scalafix] def readURL(
      url: URL,
      openConnection: URL => URLConnection
  ): String = {
    val connection = authenticatedConnection(url, openConnection)
    val src =
      scala.io.Source.fromInputStream(connection.getInputStream)("UTF-8")
    try src.getLines().mkString("\n")
    finally src.close()
  }

  private def authenticatedConnection(
      url: URL,
      openConnection: URL => URLConnection
  ): URLConnection = {
    gitHubContentsUrl(url) match {
      case Some((apiUrl, token)) =>
        val connection = openConnection(apiUrl)
        connection.setRequestProperty(
          "Accept",
          "application/vnd.github.raw+json"
        )
        connection.setRequestProperty("Authorization", s"Bearer $token")
        connection.setRequestProperty("X-GitHub-Api-Version", GitHubApiVersion)
        connection
      case None =>
        openConnection(url)
    }
  }

  private def gitHubContentsUrl(url: URL): Option[(URL, String)] =
    gitHubToken.flatMap { token =>
      val filePrefix = "/scalafix/rules/src/main/scala/"
      val path = url.getPath.stripPrefix("/")
      val filePrefixIndex = path.indexOf(filePrefix)
      if (
        url.getProtocol == "https" &&
        url.getHost == "raw.githubusercontent.com" &&
        filePrefixIndex >= 0
      ) {
        val beforeFile = path.take(filePrefixIndex)
        val coordinates = beforeFile.split("/", 3)
        if (coordinates.length == 3) {
          val owner = coordinates(0)
          val repo = coordinates(1)
          val ref = coordinates(2)
          val file = path.drop(filePrefixIndex + 1)
          Some(
            (
              new URI(
                "https",
                "api.github.com",
                s"/repos/$owner/$repo/contents/$file",
                s"ref=$ref",
                null
              ).toURL,
              token
            )
          )
        } else None
      } else None
    }

  private def gitHubToken: Option[String] =
    sys.props
      .get("scalafix.github.token")
      .orElse(sys.env.get("SCALAFIX_GITHUB_TOKEN"))
      .orElse(sys.env.get("GITHUB_TOKEN"))
      .orElse(sys.env.get("GH_TOKEN"))
      .map(_.trim)
      .filter(_.nonEmpty)

  /**
   * Reads file from file system or from http url.
   */
  def readFile(filename: String): String = {
    if (filename matches "https?://.*") {
      readURL(new URI(filename).toURL)
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
