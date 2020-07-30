package scalafix.internal.config

import java.nio.file.FileSystems
import java.nio.file.PathMatcher

import scala.meta.Dialect

import metaconfig._

case class ParserConfig(
    literalTypes: Boolean = true,
    trailingCommas: Boolean = true,
    inlineKeyword: Boolean = false
) {

  private val dialect = scala.meta.dialects.Scala213.copy(
    allowLiteralTypes = literalTypes,
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
