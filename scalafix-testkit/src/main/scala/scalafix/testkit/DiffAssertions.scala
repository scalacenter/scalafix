package scalafix.testkit

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

import org.scalatest.FunSuiteLike
import org.scalatest.exceptions.TestFailedException

object DiffAssertions {
  def compareContents(original: String, revised: String): String = {
    "".lines
    def splitLines(s: String) =
      s.trim.replaceAllLiterally("\r\n", "\n").split("\n")
    compareContents(splitLines(original), splitLines(revised))
  }

  def compareContents(original: Seq[String], revised: Seq[String]): String = {
    import collection.JavaConverters._
    val diff = difflib.DiffUtils.diff(original.asJava, revised.asJava)
    if (diff.getDeltas.isEmpty) ""
    else
      difflib.DiffUtils
        .generateUnifiedDiff(
          "expected",
          "obtained",
          original.asJava,
          diff,
          1
        )
        .asScala
        .mkString("\n")
  }
}

trait DiffAssertions extends FunSuiteLike {

  def assertEqual[A](a: A, b: A): Unit = {
    assert(a === b)
  }
  def header[T](t: T): String = {
    val line = s"=" * (t.toString.length + 3)
    s"$line\n=> $t\n$line"
  }

  case class DiffFailure(
      title: String,
      expected: String,
      obtained: String,
      diff: String
  ) extends TestFailedException(
        title + "\n--- obtained\n+++ expected\n" + error2message(
          obtained,
          expected
        ),
        3
      )

  def error2message(obtained: String, expected: String): String = {
    val sb = new StringBuilder
    if (obtained.length < 1000) {
      sb.append(s"""
                   #${header("Obtained")}
                   #${trailingSpace(obtained)}
         """.stripMargin('#'))
    }
    sb.append(s"""
                 #${header("Diff")}
                 #${trailingSpace(compareContents(obtained, expected))}
         """.stripMargin('#'))
    sb.toString()
  }
  def assertNoDiff(
      obtained: String,
      expected: String,
      title: String = ""
  ): Boolean = {
    val result = compareContents(obtained, expected)
    if (result.isEmpty) true
    else if (obtained.lines.toList == expected.lines.toList) {
      // Ignore \r\n and \n differences
      true
    } else {
      throw DiffFailure(title, expected, obtained, result)
    }
  }

  def trailingSpace(str: String): String = str.replaceAll(" \n", "∙\n")

  def compareContents(original: String, revised: String): String =
    DiffAssertions.compareContents(original, revised)

  def compareContents(original: Seq[String], revised: Seq[String]): String =
    DiffAssertions.compareContents(original, revised)

  def fileModificationTimeOrEpoch(file: File): String = {
    val format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss Z")
    if (file.exists) format.format(new Date(file.lastModified()))
    else {
      format.setTimeZone(TimeZone.getTimeZone("UTC"))
      format.format(new Date(0L))
    }
  }
}
