package scalafix.diff

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("diff", JSImport.Namespace)
object JSDiff extends js.Object {
  def createTwoFilesPatch(oldFileName: String,
                          newFileName: String,
                          oldStr: String,
                          newStr: String,
                          oldHeader: String,
                          newHeader: String,
                          options: js.Dynamic): String = js.native
}

object DiffUtils {
  def unifiedDiff(originalFileName: String,
                  revisedFileName: String,
                  originalLines: List[String],
                  revisedLines: List[String],
                  contextSize: Int): String = {
    val diff = JSDiff.createTwoFilesPatch(
      originalFileName,
      revisedFileName,
      originalLines.mkString("\n"),
      revisedLines.mkString("\n"),
      "",
      "",
      js.Dynamic.literal("context" -> contextSize))

    def trimHeader(line: String) =
      if (line.startsWith("+++") || line.startsWith("---")) line.trim else line

    def removeNewlineDiff(diffLines: Array[String]) =
      diffLines.filterNot(_ == "\\ No newline at end of file")

    removeNewlineDiff(
      diff
        .split("\n")
        .drop(1) // remove ==== separator
        .map(trimHeader) // remove whitespaces at the end of headers
    ).mkString("\n")
  }
}
