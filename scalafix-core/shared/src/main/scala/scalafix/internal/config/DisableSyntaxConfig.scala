package scalafix.internal.config

import metaconfig.{Conf, ConfError, ConfDecoder, Configured}
import org.langmeta._
import MetaconfigPendingUpstream.XtensionConfScalafix

case class DisableSyntaxConfig(
    keywords: Set[DisabledKeyword] = Set()
) {
  implicit val reader: ConfDecoder[DisableSyntaxConfig] =
    ConfDecoder.instanceF[DisableSyntaxConfig](
      _.getField(keywords).map(DisableSyntaxConfig(_))
    )
}

object DisableSyntaxConfig {
  val default = DisableSyntaxConfig()
  implicit val reader: ConfDecoder[DisableSyntaxConfig] = default.reader
}

case class DisabledKeyword(keyword: String)

object DisabledKeyword {
  private val keywords = List(
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "enum",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "forSome",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "macro",
    "new",
    "null",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "this",
    "throw",
    "trait",
    "true",
    "try",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield"
  )
  private val keywordsSet = keywords.toSet

  implicit val reader: ConfDecoder[DisabledKeyword] =
    new ConfDecoder[DisabledKeyword] {
      override def read(conf: Conf): Configured[DisabledKeyword] = {
        def readKeyword(keyword: String): Configured[DisabledKeyword] = {
          if (keywordsSet.contains(keyword))
            Configured.Ok(DisabledKeyword(keyword))
          else Configured.NotOk(oneOfTypo(keyword, conf))
        }
        conf match {
          case Conf.Null() => readKeyword("null")
          case Conf.Str(keyword) => readKeyword(keyword)
          case Conf.Bool(value) =>
            if (value) readKeyword("true")
            else readKeyword("false")

          case _ => Configured.typeMismatch("String", conf)
        }
      }
    }

  // XXX: This is from metaconfig.ConfError
  def oneOfTypo(keyword: String, conf: Conf): ConfError = {
    val closestKeyword = keywords.minBy(levenshtein(keyword))
    val relativeDistance =
      levenshtein(keyword)(closestKeyword) /
        keyword.length.toDouble

    val didYouMean =
      if (relativeDistance < 0.20) s" (Did you mean: $closestKeyword?)"
      else ""

    ConfError.msg(s"$keyword is not in our supported keywords.$didYouMean")
  }

  /** Levenshtein distance. Implementation based on Wikipedia's algorithm. */
  private def levenshtein(s1: String)(s2: String): Int = {
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) { (j, i) =>
      if (j == 0) i else if (i == 0) j else 0
    }

    for (j <- 1 to s2.length; i <- 1 to s1.length)
      dist(j)(i) =
        if (s2(j - 1) == s1(i - 1))
          dist(j - 1)(i - 1)
        else
          dist(j - 1)(i)
            .min(dist(j)(i - 1))
            .min(dist(j - 1)(i - 1)) + 1

    dist(s2.length)(s1.length)
  }
}
