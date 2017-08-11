package scalafix
package internal.config

import java.io.PrintStream
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import metaconfig._

case class ScalafixConfig(
    parser: Parse[_ <: Tree] = Parse.parseSource,
    debug: DebugConfig = DebugConfig(),
    groupImportsByPrefix: Boolean = true,
    fatalWarnings: Boolean = true,
    reporter: ScalafixReporter = ScalafixReporter.default,
    patches: ConfigRewritePatches = ConfigRewritePatches.default,
    dialect: Dialect = Scala211,
    // Custom configuration for rewrites.
    // Feel free to read data from here if your custom rewrite needs
    // configuration from the user.
    x: Conf = Conf.Obj(),
    explicitReturnTypes: ExplicitReturnTypesConfig = ExplicitReturnTypesConfig()
) {
  def getRewriteConfig[T: ConfDecoder](key: String, default: T): T = {
    x.getOrElse[T](key)(default).get
  }

  val reader: ConfDecoder[ScalafixConfig] =
    ConfDecoder.instanceF[ScalafixConfig] { conf =>
      import conf._
      (
        getOrElse("fatalWarnings")(fatalWarnings) |@|
          getOrElse("reporter")(reporter) |@|
          getOrElse("patches")(patches)(patches.reader) |@|
          getOrElse("dialect")(dialect) |@|
          getOrElse("x")(x) |@|
          getOrElse("explicitReturnTypes")(explicitReturnTypes)
      ).map {
        case (((((a, b), c), d), e), f) =>
          copy(
            fatalWarnings = a,
            reporter = b,
            patches = c,
            dialect = d,
            x = e,
            explicitReturnTypes = f
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

  /** Returns config from current working directory, if .scalafix.conf exists. */
  def auto(workingDirectory: AbsolutePath): Option[Input] = {
    val file = workingDirectory.resolve(".scalafix.conf")
    if (file.isFile && file.toFile.exists())
      Some(Input.File(file))
    else None
  }

  def fromInput(
      input: Input,
      semanticCtx: LazySemanticCtx,
      extraRewrites: List[String] = Nil)(
      implicit decoder: ConfDecoder[Rewrite]
  ): Configured[(Rewrite, ScalafixConfig)] =
    configFromInput(input, semanticCtx, extraRewrites)

}
