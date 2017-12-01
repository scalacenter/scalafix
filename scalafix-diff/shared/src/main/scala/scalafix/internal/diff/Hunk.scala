package scalafix.internal.diff

private[scalafix] case class Hunk(
    originalLine: Int,
    originalCount: Int,
    revisedLine: Int,
    revisedCount: Int)

private[scalafix] object HunkExtractor {
  private val HunkHeader =
    "^@@ -(\\d+)(,(\\d+))? \\+(\\d+)(,(\\d+))? @@(.*)$".r
  def unapply(line: String): Option[Hunk] = {
    line match {
      case HunkHeader(
          originalLine,
          _,
          originalCount,
          revisedLine,
          _,
          revisedCount,
          _) => {
        Some(
          Hunk(
            originalLine.toInt,
            Option(originalCount).map(_.toInt).getOrElse(1),
            revisedLine.toInt,
            Option(revisedCount).map(_.toInt).getOrElse(1)
          ))
      }
      case _ => None
    }
  }
}