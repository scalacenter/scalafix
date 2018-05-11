package scalafix.reflect

import metaconfig.Conf
import scalafix.SemanticdbIndex
import scalafix.Rule
import scalafix.internal.config._
import scalafix.internal.reflect.ScalafixCompilerDecoder
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import scalafix.v1
import scalafix.internal.v1.Rules

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
object ScalafixReflectV1 { self =>
  def read(rule: String): Configured[v1.Rule] =
  // TODO: handle github: class: file:
    Rules.defaults.find(_.name.matches(rule)) match {
      case Some(r) => Configured.ok(r)
      case _ => ConfError.message(s"Invalid rule name $rule").notOk
    }
  def decoder: ConfDecoder[Rules] = new ConfDecoder[Rules] {
    override def read(conf: Conf): Configured[Rules] = conf match {
      case str: Conf.Str =>
        read(Conf.Lst(str :: Nil))
      case Conf.Lst(values) =>
        val decoded = values.map {
          case Conf.Str(value) =>
            self.read(value)
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
