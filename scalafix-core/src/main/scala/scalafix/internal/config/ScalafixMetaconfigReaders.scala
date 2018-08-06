package scalafix.internal.config

import scala.meta.Ref
import scala.meta._
import scala.meta.parsers.Parse
import scalafix.v0.Symbol
import scala.reflect.ClassTag
import scala.util.Try
import scala.util.matching.Regex
import scalafix.patch.TreePatch._
import java.io.OutputStream
import java.io.PrintStream
import java.net.URI
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import scala.util.control.NonFatal
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Configured.Ok
import scalafix.v0._

object ScalafixMetaconfigReaders extends ScalafixMetaconfigReaders
// A collection of metaconfig.Reader instances that are shared across
trait ScalafixMetaconfigReaders {

  implicit lazy val parseReader: ConfDecoder[MetaParser] = {
    import scala.meta.parsers.Parse._
    ReaderUtil.oneOf[MetaParser](parseSource, parseStat, parseCase)
  }
  implicit lazy val dialectReader: ConfDecoder[Dialect] = {
    import scala.meta.dialects._
    import ScalafixConfig.{DefaultDialect => Default}
    ReaderUtil.oneOf[Dialect](
      Default,
      Scala211,
      Scala212,
      Sbt0137,
      Sbt1,
      Dotty,
      Paradise211,
      Paradise212
    )
  }

  object UriRuleString {
    def unapply(arg: Conf.Str): Option[(String, String)] =
      UriRule.unapply(arg).map {
        case (a, b) => a -> b.getSchemeSpecificPart
      }
  }

  object UriRule {
    def unapply(arg: Conf.Str): Option[(String, URI)] =
      for {
        uri <- Try(new URI(arg.value)).toOption
        scheme <- Option(uri.getScheme)
      } yield scheme -> uri
  }

  private val ruleRegex = Pattern.compile("(rules?|rewrites?)")
  private def isRuleKey(key: (String, Conf)) =
    ruleRegex.matcher(key._1).matches()
  def scalafixConfigEmptyRuleReader: ConfDecoder[(Conf, ScalafixConfig)] =
    ConfDecoder.instance[(Conf, ScalafixConfig)] {
      case Conf.Obj(values) =>
        val (rules, noRules) = values.partition(isRuleKey)
        val ruleConf =
          Configured.Ok(rules.lastOption.map(_._2).getOrElse(Conf.Lst()))
        val config =
          ScalafixConfig.ScalafixConfigDecoder.read(Conf.Obj(noRules))
        ruleConf.product(config)
    }
  def scalafixConfigConfDecoder(
      ruleDecoder: ConfDecoder[Rule],
      extraRules: List[String] = Nil
  ): ConfDecoder[(Rule, ScalafixConfig)] =
    scalafixConfigEmptyRuleReader.flatMap {
      case (ruleConf, config) =>
        val combinedRules: Conf.Lst =
          if (extraRules.nonEmpty)
            Conf.Lst(extraRules.map(Conf.Str))
          else
            ruleConf match {
              case rules @ Conf.Lst(_) => rules
              case x => Conf.Lst(x :: Nil)
            }
        ruleDecoder.read(combinedRules).map(rule => rule -> config)
    }

  private lazy val semanticRuleClass = classOf[SemanticRule]

  lazy val SlashSeparated: Regex = "([^/]+)/(.*)".r

  def parseReplaceSymbol(
      from: String,
      to: String): Configured[(Symbol.Global, Symbol.Global)] =
    symbolGlobalReader.read(Conf.Str(from)) |@|
      symbolGlobalReader.read(Conf.Str(to))

  implicit lazy val ReplaceSymbolReader: ConfDecoder[ReplaceSymbol] =
    ConfDecoder.instanceF[ReplaceSymbol] { c =>
      (
        c.get[Symbol.Global]("from") |@|
          c.get[Symbol.Global]("to")
      ).map { case (a, b) => ReplaceSymbol(a, b) }
    }

  def ruleConfDecoderSyntactic(
      singleRuleDecoder: ConfDecoder[Rule]): ConfDecoder[Rule] =
    ruleConfDecoder(singleRuleDecoder)
  def ruleConfDecoder(
      singleRuleDecoder: ConfDecoder[Rule]): ConfDecoder[Rule] = {
    ConfDecoder.instance[Rule] {
      case Conf.Lst(values) =>
        MetaconfigPendingUpstream
          .flipSeq(values.map(singleRuleDecoder.read))
          .map(rules => Rule.combine(rules))
      case rule @ Conf.Str(_) => singleRuleDecoder.read(rule)
    }
  }

  object ConfStrLst {
    def unapply(arg: Conf.Lst): Option[List[String]] =
      if (arg.values.forall(_.isInstanceOf[Conf.Str]))
        Some(arg.values.collect { case Conf.Str(value) => value })
      else None
  }

  implicit val RegexReader: ConfDecoder[Regex] = ConfDecoder.instance[Regex] {
    case Conf.Str(str) => Configured.Ok(FilterMatcher.mkRegexp(List(str)))
    case ConfStrLst(values) => Configured.Ok(FilterMatcher.mkRegexp(values))
  }
  implicit val FilterMatcherReader: ConfDecoder[FilterMatcher] =
    ConfDecoder.instance[FilterMatcher] {
      case Conf.Str(str) => Configured.Ok(FilterMatcher(str))
      case ConfStrLst(values) =>
        Configured.Ok(FilterMatcher(values, Nil))
      case els =>
        FilterMatcher.matchNothing.reader.read(els)
    }

  def parseReader[T](implicit parse: Parse[T]): ConfDecoder[T] =
    ConfDecoder.stringConfDecoder.flatMap { str =>
      str.parse[T] match {
        case parsers.Parsed.Success(x) => Configured.Ok(x)
        case parsers.Parsed.Error(pos, msg, _) =>
          import MetaconfigPendingUpstream._
          ConfError.parseError(pos.toMetaconfig, msg).notOk
      }
    }

  def castReader[From, To](ConfDecoder: ConfDecoder[From])(
      implicit ev: ClassTag[To]): ConfDecoder[To] = ConfDecoder.flatMap {
    case x if ev.runtimeClass.isInstance(x) =>
      Configured.Ok(x.asInstanceOf[To])
    case x =>
      ConfError.message(s"Expected Ref, got ${x.getClass}").notOk
  }
  implicit lazy val importerReader: ConfDecoder[Importer] =
    parseReader[Importer]
  implicit lazy val importeeReader: ConfDecoder[Importee] =
    parseReader[Importee]
  implicit lazy val refReader: ConfDecoder[Ref] =
    castReader[Stat, Ref](parseReader[Stat])
  implicit lazy val termRefReader: ConfDecoder[Term.Ref] =
    castReader[Stat, Term.Ref](parseReader[Stat])
  implicit lazy val symbolReader: ConfDecoder[Symbol] =
    ConfDecoder.stringConfDecoder.map(Symbol.apply)
  private def parseSymbol(sym: String): Configured[Symbol] =
    try Ok(Symbol(sym)) // Because https://github.com/scalameta/scalameta/issues/821
    catch { case NonFatal(e) => ConfError.exception(e, 0).notOk }
  implicit lazy val symbolGlobalReader: ConfDecoder[Symbol.Global] =
    ConfDecoder.instance[Symbol.Global] {
      case Conf.Str(path) =>
        def symbolGlobal(symbol: Symbol): Configured[Symbol.Global] =
          symbol match {
            case g: Symbol.Global => Ok(g)
            case els =>
              ConfError
                .typeMismatch(
                  "Symbol.Global",
                  Conf.Str(s"$els: ${els.productPrefix}"))
                .notOk
          }
        var toParse = path
        if (!path.endsWith(".") && !path.endsWith("#")) toParse += "."
        parseSymbol(toParse).andThen(symbolGlobal)
    }
  implicit lazy val AddGlobalImportReader: ConfDecoder[AddGlobalImport] =
    importerReader.map(AddGlobalImport.apply)
  implicit lazy val RemoveGlobalImportReader: ConfDecoder[RemoveGlobalImport] =
    termRefReader.flatMap { ref =>
      parseSymbol(ref.syntax + ".").map { s =>
        RemoveGlobalImport(s)
      }
    }

  implicit val metaconfigConfDecoder: ConfDecoder[Conf] =
    new ConfDecoder[Conf] {
      override def read(conf: Conf): Configured[Conf] = Ok(conf)
    }

  implicit lazy val PrintStreamReader: ConfDecoder[PrintStream] = {
    val empty = new PrintStream(new OutputStream {
      override def write(b: Int): Unit = ()
    })
    ReaderUtil.oneOf[PrintStream](empty)
  }

  implicit lazy val PatternDecoder: ConfDecoder[Pattern] = {
    ConfDecoder.stringConfDecoder.flatMap(pattern =>
      try {
        Configured.Ok(Pattern.compile(pattern, Pattern.MULTILINE))
      } catch {
        case ex: PatternSyntaxException =>
          Configured.NotOk(ConfError.message(ex.getMessage))
    })
  }

  implicit lazy val CustomMessagePattern: ConfDecoder[CustomMessage[Pattern]] =
    CustomMessage.decoder(field = "pattern")

}
