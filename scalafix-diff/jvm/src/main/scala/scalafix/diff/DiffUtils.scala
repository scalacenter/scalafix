package scalafix.diff

import difflib.{DiffUtils => DU}
import scala.collection.JavaConverters._

object DiffUtils {
  def unifiedDiff(
      originalFileName: String,
      revisedFileName: String,
      originalLines: List[String],
      revisedLines: List[String],
      contextSize: Int): String = {
    val patch = DU.diff(originalLines.toSeq.asJava, revisedLines.toSeq.asJava)
    val diff = DU.generateUnifiedDiff(
      originalFileName,
      revisedFileName,
      originalLines.toSeq.asJava,
      patch,
      contextSize)
    diff.asScala.mkString("\n")
  }
}
