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
                          oldHeader: Option[String],
                          newHeader: Option[String],
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
      None,
      None,
      js.Dynamic.literal("context" -> contextSize))
    diff.split("\n").drop(1).mkString("\n")
  }
}
