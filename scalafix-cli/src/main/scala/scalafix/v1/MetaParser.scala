package scalafix.v1

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import metaconfig._
import scala.meta.Dialect

case class MetaParser(
    trailingCommas: Boolean = true,
    inlineKeyword: Boolean = false
) {

  private val dialect = scala.meta.dialects.Scala212.copy(
    allowTrailingCommas = trailingCommas,
    allowInlineIdents = inlineKeyword
  )
  private val sbtDialect = dialect.copy(allowToplevelTerms = true)

  def dialectForFile(path: String): Dialect =
    if (path.endsWith(".sbt")) sbtDialect
    else dialect

}

object MetaParser {
  val sbtMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:*.sbt")
  implicit val surface: generic.Surface[MetaParser] =
    generic.deriveSurface
  implicit val decoder: ConfDecoder[MetaParser] =
    generic.deriveDecoder(MetaParser())
}
