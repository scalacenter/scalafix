package scalafix
package internal.config

import java.io.OutputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.dialects.Scala212
import scala.meta.parsers.Parse
import metaconfig._

case class ScalafixConfig(
    parser: Parse[_ <: Tree] = Parse.parseSource,
    debug: DebugConfig = DebugConfig(),
    groupImportsByPrefix: Boolean = true,
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    patches: ConfigRulePatches = ConfigRulePatches.default,
    dialect: Dialect = ScalafixConfig.DefaultDialect,
    lint: LintConfig = LintConfig.default
) {

  def withFreshReporters: ScalafixConfig = copy(
    reporter = reporter.reset,
    lint = lint.copy(
      reporter = lint.reporter.reset
    )
  )

  def withFreshReporters(outStream: OutputStream): ScalafixConfig = copy(
    reporter = reporter.reset(outStream),
    lint = lint.copy(
      reporter = lint.reporter.reset(outStream)
    )
  )

  val reader: ConfDecoder[ScalafixConfig] =
    ConfDecoder.instanceF[ScalafixConfig] { conf =>
      import conf._
      (
        getOrElse("fatalWarnings")(fatalWarnings) |@|
          getOrElse("reporter")(reporter) |@|
          getOrElse("patches")(patches)(patches.reader) |@|
          getOrElse("dialect")(dialect) |@|
          getOrElse("lint")(lint)(lint.reader)
      ).map {
        case ((((a, b), c), d), e) =>
          copy(
            fatalWarnings = a,
            reporter = b,
            patches = c,
            dialect = d,
            lint = e
          )
      }

    }

  def withOut(out: PrintStream): ScalafixConfig = copy(
    reporter = reporter match {
      case r: PrintStreamReporter => r.copy(outStream = out)
      case _ => ScalafixReporter.default.copy(outStream = out)
    }
  )
}

object ScalafixConfig {

  lazy val default = ScalafixConfig()
  implicit lazy val ScalafixConfigDecoder: ConfDecoder[ScalafixConfig] =
    default.reader

  val DefaultDialect: Dialect = Scala212.copy(
    // Are `&` intersection types supported by this dialect?
    allowAndTypes = true,
    // Are extractor varargs specified using ats, i.e. is `case Extractor(xs @ _*)` legal or not?
    allowAtForExtractorVarargs = true,
    // Are extractor varargs specified using colons, i.e. is `case Extractor(xs: _*)` legal or not?
    allowColonForExtractorVarargs = true,
    // Are inline vals and defs supported by this dialect?
    allowInlineMods = true,
    // Are literal types allowed, i.e. is `val a : 42 = 42` legal or not?
    allowLiteralTypes = true,
    // Are `|` (union types) supported by this dialect?
    allowOrTypes = true,
    // Are trailing commas allowed? SIP-27.
    allowTrailingCommas = true,
    // Are trait allowed to have parameters?
    // They are in Dotty, but not in Scala 2.12 or older.
    allowTraitParameters = true,
    // Are view bounds supported by this dialect?
    // Removed in Dotty.
    allowViewBounds = true,
    // Are `with` intersection types supported by this dialect?
    allowWithTypes = true,
    // Are XML literals supported by this dialect?
    // We plan to deprecate XML literal syntax, and some dialects
    // might go ahead and drop support completely.
    allowXmlLiterals = true
  )

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDirectory: AbsolutePath): Option[Input] = {
    val file = workingDirectory.resolve(".scalafix.conf")
    if (file.isFile && file.toFile.exists())
      Some(Input.File(file))
    else None
  }

  def fromInput(
      input: Input,
      index: LazySemanticdbIndex,
      extraRules: List[String] = Nil)(
      implicit decoder: ConfDecoder[Rule]
  ): Configured[(Rule, ScalafixConfig)] =
    configFromInput(input, index, extraRules)

}
