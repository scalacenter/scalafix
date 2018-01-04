package scalafix
package internal.config

import metaconfig.{Conf, ConfError, ConfDecoder, Configured}
import MetaconfigPendingUpstream.XtensionConfScalafix

import scala.meta.tokens.Token

import java.util.regex.{Pattern, PatternSyntaxException}

case class DisableSyntaxConfig(
    keywords: Set[DisabledKeyword] = Set(),
    noSemicolons: Boolean = false,
    noTabs: Boolean = false,
    noXml: Boolean = false,
    noCovariantTypes: Boolean = false,
    noContravariantTypes: Boolean = false,
    regex: List[CustomMessage[Pattern]] = Nil
) {
  implicit val reader: ConfDecoder[DisableSyntaxConfig] =
    ConfDecoder.instanceF[DisableSyntaxConfig](
      c =>
        (
          c.getField(keywords) |@|
            c.getField(noSemicolons) |@|
            c.getField(noTabs) |@|
            c.getField(noXml) |@|
            c.getField(noCovariantTypes) |@|
            c.getField(noContravariantTypes) |@|
            c.getField(regex)
        ).map {
          case ((((((a, b), c), d), e), f), g) =>
            DisableSyntaxConfig(a, b, c, d, e, f, g)
      })

  implicit val patternReader: ConfDecoder[Pattern] = {
    ConfDecoder.stringConfDecoder.flatMap(pattern =>
      try {
        Configured.Ok(Pattern.compile(pattern, Pattern.MULTILINE))
      } catch {
        case ex: PatternSyntaxException =>
          Configured.NotOk(ConfError.msg(ex.getMessage))
    })
  }

  implicit val customMessageReader: ConfDecoder[CustomMessage[Pattern]] =
    CustomMessage.decoder(field = "pattern")

  def isDisabled(keyword: String): Boolean =
    keywords.contains(DisabledKeyword(keyword))
}

object DisableSyntaxConfig {
  val default: DisableSyntaxConfig = DisableSyntaxConfig()
  implicit val reader: ConfDecoder[DisableSyntaxConfig] = default.reader
}

object Keyword {
  def unapply(token: Token): Option[String] = {
    val prefix = token.productPrefix
    if (kwSet.contains(prefix)) {
      Some(prefix.drop(2).toLowerCase)
    } else {
      None
    }
  }
  val all: List[String] = List(
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
  val set: Set[String] = all.toSet
  private val kwSet = all.map(kw => s"Kw${kw.capitalize}").toSet
}

case class DisabledKeyword(keyword: String)

object DisabledKeyword {
  implicit val reader: ConfDecoder[DisabledKeyword] =
    new ConfDecoder[DisabledKeyword] {
      override def read(conf: Conf): Configured[DisabledKeyword] = {
        def readKeyword(keyword: String): Configured[DisabledKeyword] = {
          if (Keyword.set.contains(keyword))
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
    val closestKeyword = Keyword.all.minBy(levenshtein(keyword))
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
