package scalafix

import scala.collection.immutable.Seq
import scala.meta._
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scala.util.control.NonFatal
import scalafix.rewrite.Rewrite
import scalafix.syntax._

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

case class ImportsConfig(
    expandRelative: Boolean = true,
    spaceAroundCurlyBrace: Boolean = false,
    organize: Boolean = true,
    removeUnused: Boolean = true,
    alwaysUsed: Seq[Ref] = Seq(),
    groups: Seq[FilterMatcher] = Seq(
      FilterMatcher("scala.language.*"),
      FilterMatcher("(scala|scala\\..*)$"),
      FilterMatcher("(java|java\\..*)$"),
      FilterMatcher(".*")
    ),
    groupByPrefix: Boolean = false
)
object ImportsConfig {
  def default: ImportsConfig = ImportsConfig()
}
case class ScalafixConfig(
    rewrites: Seq[Rewrite] = Rewrite.defaultRewrites,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    imports: ImportsConfig = ImportsConfig(),
    fatalWarning: Boolean = true,
    dialect: Dialect = Scala211
)

object ScalafixConfig {
  private def saferThanTypesafe(
      op: () => Config): Either[String, ScalafixConfig] =
    try fromConfig(op())
    catch {
      case NonFatal(e) => Left(e.getMessage)
    }
  val default = ScalafixConfig()

  def fromFile(file: File): Either[String, ScalafixConfig] =
    saferThanTypesafe(() => ConfigFactory.parseFile(file))

  def fromString(contents: String): Either[String, ScalafixConfig] =
    saferThanTypesafe(() => ConfigFactory.parseString(contents))

  def fromConfig(config: Config): Either[String, ScalafixConfig] = {
    import scala.collection.JavaConverters._
    val base = ScalafixConfig(
      fatalWarning = config.getBoolOrElse("fatalWarnings",
                                          ScalafixConfig.default.fatalWarning),
      imports = ImportsConfig(
        expandRelative =
          config.getBoolOrElse("imports.expandRelative",
                               ImportsConfig.default.expandRelative),
        spaceAroundCurlyBrace =
          config.getBoolOrElse("imports.spaceAroundCurlyBrace",
                               ImportsConfig.default.spaceAroundCurlyBrace),
        organize = config.getBoolOrElse("imports.organize",
                                        ImportsConfig.default.organize),
        removeUnused =
          config.getBoolOrElse("imports.removeUnused",
                               ImportsConfig.default.removeUnused),
        groupByPrefix =
          config.getBoolOrElse("imports.groupByPrefix",
                               ImportsConfig.default.groupByPrefix)
      )
    )
    if (config.hasPath("rewrites"))
      fromNames(config.getStringList("rewrites").asScala.toList).right
        .map(rewrites => base.copy(rewrites = rewrites))
    else Right(ScalafixConfig())
  }

  def fromNames(names: List[String]): Either[String, Seq[Rewrite]] =
    names match {
      case "all" :: Nil => Right(Rewrite.allRewrites)
      case _ =>
        val invalidNames =
          names.filterNot(Rewrite.name2rewrite.contains)
        if (invalidNames.nonEmpty) {
          Left(
            s"Invalid rewrite rule: ${invalidNames.mkString(",")}. " +
              s"Valid rules are: ${Rewrite.name2rewrite.keys.mkString(",")}")
        } else {
          Right(names.map(Rewrite.name2rewrite))
        }
    }
}
