package scalafix.internal.diff

import difflib.{DiffUtils => DU}
import scala.jdk.CollectionConverters._

object DiffUtils {
  def unifiedDiff(
      originalFileName: String,
      revisedFileName: String,
      originalLines: List[String],
      revisedLines: List[String],
      contextSize: Int
  ): String = {
    val patch = DU.diff(originalLines.asJava, revisedLines.asJava)
    val diff = DU.generateUnifiedDiff(
      originalFileName,
      revisedFileName,
      originalLines.asJava,
      patch,
      contextSize
    )
    diff.asScala.mkString("\n")
  }
}
