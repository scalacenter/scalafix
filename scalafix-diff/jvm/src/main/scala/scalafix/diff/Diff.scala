package scalafix.diff

import scala.collection.JavaConverters._

object DiffUtils {
  def unifiedDiff(originalFileName: String,
                  revisedFileName: String,
                  originalLines: List[String],
                  revisedLines: List[String],
                  contextSize: Int): String = {
    val patch = difflib.DiffUtils
      .diff(originalLines.toSeq.asJava, revisedLines.toSeq.asJava)
    val diff = difflib.DiffUtils.generateUnifiedDiff(
      originalFileName,
      revisedFileName,
      originalLines.toSeq.asJava,
      patch,
      contextSize)
    diff.asScala.mkString("\n")
  }
}
