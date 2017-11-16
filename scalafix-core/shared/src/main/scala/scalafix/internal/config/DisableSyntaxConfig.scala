package scalafix.internal.config

import metaconfig.{Conf, ConfError, ConfDecoder, Configured}
import org.langmeta._
import MetaconfigPendingUpstream.XtensionConfScalafix

import scala.meta.tokens.Token

case class DisableSyntaxConfig(
    keywords: Set[DisabledKeyword] = Set(),
    tabs: Boolean = false,
    semicolons: Boolean = false,
    xml: Boolean = false
) {
  implicit val reader: ConfDecoder[DisableSyntaxConfig] =
    ConfDecoder.instanceF[DisableSyntaxConfig](c =>
      (
        c.getField(keywords) |@|
          c.getField(tabs) |@|
          c.getField(semicolons) |@|
          c.getField(xml)
      ).map {
        case (((a, b), c), d) =>
          DisableSyntaxConfig(a, b, c, d)
      }
    )

  def isDisabled(keyword: String): Boolean =
    keywords.contains(DisabledKeyword(keyword))
}

object DisableSyntaxConfig {
  val default = DisableSyntaxConfig()
  implicit val reader: ConfDecoder[DisableSyntaxConfig] = default.reader
}

case class DisabledKeyword(keyword: String)

object DisabledKeyword {
  def unapply(token: Token): Option[String] = {
    import Token._

    token match {
      case KwAbstract()  => Some("abstract")
      case KwCase()      => Some("case")
      case KwCatch()     => Some("catch")
      case KwClass()     => Some("class")
      case KwDef()       => Some("def")
      case KwDo()        => Some("do")
      case KwElse()      => Some("else")
      case KwEnum()      => Some("enum")
      case KwExtends()   => Some("extends")
      case KwFalse()     => Some("false")
      case KwFinal()     => Some("final")
      case KwFinally()   => Some("finally")
      case KwFor()       => Some("for")
      case KwForsome()   => Some("forSome")
      case KwIf()        => Some("if")
      case KwImplicit()  => Some("implicit")
      case KwImport()    => Some("import")
      case KwLazy()      => Some("lazy")
      case KwMatch()     => Some("match")
      case KwMacro()     => Some("macro")
      case KwNew()       => Some("new")
      case KwNull()      => Some("null")
      case KwObject()    => Some("object")
      case KwOverride()  => Some("override")
      case KwPackage()   => Some("package")
      case KwPrivate()   => Some("private")
      case KwProtected() => Some("protected")
      case KwReturn()    => Some("return")
      case KwSealed()    => Some("sealed")
      case KwSuper()     => Some("super")
      case KwThis()      => Some("this")
      case KwThrow()     => Some("throw")
      case KwTrait()     => Some("trait")
      case KwTrue()      => Some("true")
      case KwTry()       => Some("try")
      case KwType()      => Some("type")
      case KwVal()       => Some("val")
      case KwVar()       => Some("var")
      case KwWhile()     => Some("while")
      case KwWith()      => Some("with")
      case KwYield()     => Some("yield")
      case _             => None
    }
  }
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

  // per file:
  //   Space    Space
  //   Tab      Tab
  //   CR       CarriageReturn
  //   LF       LineFeed
  //   FF       FormFeed

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
