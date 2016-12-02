package scalafix

import scala.meta.Dialect
import scala.meta.Tree
import scala.meta.dialects.Scala211
import scala.meta.parsers.Parse
import scalafix.rewrite.Rewrite

case class ScalafixConfig(
    rewrites: Seq[Rewrite] = Rewrite.allRewrites,
    parser: Parse[_ <: Tree] = Parse.parseSource,
    dialect: Dialect = Scala211
)

object ScalafixConfig {
  def fromNames(names: List[String]): Either[String, ScalafixConfig] = {
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
          Right(ScalafixConfig(rewrites = rewrites))
        }
    }
  }
}
