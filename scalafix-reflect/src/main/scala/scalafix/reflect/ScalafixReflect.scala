package scalafix.reflect

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import scalafix.Rule
import scalafix.SemanticdbIndex
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import scalafix.internal.v1.Rules
import scalafix.v1

object ScalafixReflect {
  def syntactic: ConfDecoder[Rule] =
    fromLazySemanticdbIndex(LazySemanticdbIndex.empty)

  def semantic(index: SemanticdbIndex): ConfDecoder[Rule] =
    fromLazySemanticdbIndex(LazySemanticdbIndex(_ => Some(index)))

  def fromLazySemanticdbIndex(index: LazySemanticdbIndex): ConfDecoder[Rule] =
    ruleConfDecoder(
      ScalafixCompilerDecoder
        .baseCompilerDecoder(index)
        .orElse(baseRuleDecoders(index))
    )
}
object ScalafixReflectV1 {

  def readSingleRule(
      rule: String,
      classloader: ClassLoader): Configured[v1.Rule] =
    // TODO: handle github: class: file:
    Rules.defaults.find(_.name.matches(rule)) match {
      case Some(r) => Configured.ok(r)
      case _ =>
        Conf.Str(rule) match {
          case UriRuleString("scala" | "class", fqn) =>
            tryClassload(classloader, fqn) match {
              case Some(cls) =>
                Configured.ok(cls.newInstance().asInstanceOf[v1.Rule])
              case _ =>
                ConfError.message(s"Class not found: $fqn").notOk
            }
        }
    }

  def tryClassload(classloader: ClassLoader, fqn: String): Option[Class[_]] = {
    try Some(classloader.loadClass(fqn))
    catch {
      case _: ClassNotFoundException =>
        try Some(classloader.loadClass(fqn + "$"))
        catch {
          case _: ClassNotFoundException =>
            None
        }
    }
  }

  def decoder(
      reporter: ScalafixReporter,
      classloader: ClassLoader): ConfDecoder[Rules] =
    new ConfDecoder[Rules] {
      override def read(conf: Conf): Configured[Rules] = conf match {
        case str: Conf.Str =>
          read(Conf.Lst(str :: Nil))
        case Conf.Lst(values) =>
          val decoded = values.map {
            case Conf.Str(value) =>
              readSingleRule(value, classloader).map { r =>
                r.name.reportDeprecationWarning(value, reporter)
                r
              }
            case err =>
              ConfError.typeMismatch("String", err).notOk
          }
          MetaconfigPendingUpstream.flipSeq(decoded).map { rules =>
            Rules(rules.toList)
          }
        case els =>
          ConfError.typeMismatch("Either[String, List[String]]", els).notOk
      }
    }

}
