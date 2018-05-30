package scalafix.internal.config

import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import metaconfig._
import scala.meta.Dialect

case class ParserConfig(
    trailingCommas: Boolean = true,
    inlineKeyword: Boolean = false
) {

  private val dialect = scala.meta.dialects.Scala212.copy(
    allowTrailingCommas = trailingCommas,
    allowInlineMods = inlineKeyword,
    allowInlineIdents = !inlineKeyword
  )
  private val sbtDialect = dialect.copy(allowToplevelTerms = true)

  def dialectForFile(path: String): Dialect =
    if (path.endsWith(".sbt")) sbtDialect
    else dialect

}

object ParserConfig {
  val sbtMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:*.sbt")
  implicit val surface: generic.Surface[ParserConfig] =
    generic.deriveSurface
  implicit val codec: ConfCodec[ParserConfig] =
    generic.deriveCodec(ParserConfig())
}
