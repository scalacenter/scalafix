package scalafix

import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scala.util.control.NonFatal
import scalafix.rewrite.Rewrite
import scalafix.util.logger

import java.io.File

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

case class ScalafixConfig(
    rewrites: Seq[Rewrite] = Rewrite.allRewrites,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    dialect: Dialect = Scala211
)

object ScalafixConfig {
  private def saferThanTypesafe(
      op: () => Config): Either[String, ScalafixConfig] =
    try fromConfig(op())
    catch {
      case NonFatal(e) => Left(e.getMessage)
    }

  def fromFile(file: File): Either[String, ScalafixConfig] =
    saferThanTypesafe(() => ConfigFactory.parseFile(file))

  def fromString(contents: String): Either[String, ScalafixConfig] =
    saferThanTypesafe(() => ConfigFactory.parseString(contents))

  def fromConfig(config: Config): Either[String, ScalafixConfig] = {
    import scala.collection.JavaConverters._
    if (config.hasPath("rewrites"))
      fromNames(config.getStringList("rewrites").asScala.toList)
    else Right(ScalafixConfig())
  }

  def fromNames(names: List[String]): Either[String, ScalafixConfig] =
    names match {
      case "all" :: Nil =>
        Right(ScalafixConfig(rewrites = Rewrite.allRewrites))
      case _ =>
        val invalidNames =
          names.filterNot(Rewrite.name2rewrite.contains)
        if (invalidNames.nonEmpty) {
          Left(
            s"Invalid rewrite rule: ${invalidNames.mkString(",")}. " +
              s"Valid rules are: ${Rewrite.name2rewrite.keys.mkString(",")}")
        } else {
          val rewrites = names.map(Rewrite.name2rewrite)
          logger.elem(rewrites)
          Right(ScalafixConfig(rewrites = rewrites))
        }
    }
}
