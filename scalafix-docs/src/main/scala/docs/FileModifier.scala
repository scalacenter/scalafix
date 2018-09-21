package docs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import mdoc.Reporter
import mdoc.StringModifier
import scala.meta.inputs.Input
import scala.meta.inputs.Position

class FileModifier extends StringModifier {
  override val name: String = "file"
  override def process(
      info: String,
      code: Input,
      reporter: Reporter): String = {
    val filename = info
    val path = Paths.get(filename)
    val text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    val input = Input.VirtualFile(filename, text)
    code.text.linesIterator.toList match {
      case List(startMarker, endMarker) =>
        val start = text.indexOf(startMarker.stripSuffix("..."))
        val end = text.indexOf(endMarker.stripPrefix("..."))
        val pos = Position.Range(input, start, end)
        if (end < 0) {
          reporter.error(s"not found '$startMarker'")
          ""
        } else if (end < 0) {
          reporter.error(s"not found '$endMarker'")
          ""
        } else {
          val linePos =
            Position.Range(input, pos.startLine, 0, pos.endLine, Int.MaxValue)
          "```scala\n" + linePos.text + "\n```"
        }
      case els =>
        val pos = Position.Range(code, 0, 0)
        val msg = s"Expected code fence to have two lines. Obtained: $els"
        reporter.error(pos, msg)
        msg
    }
  }

}
