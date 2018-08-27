package scalafix.internal.rule

import metaconfig.Configured
import scala.meta._
import scalafix.v0.LintCategory
import scalafix.v1._

final case class DisableSyntax(config: DisableSyntaxConfig)
    extends SyntacticRule("DisableSyntax")
    with Product {

  def this() = this(DisableSyntaxConfig())

  override def description: String =
    "Linter that reports an error on a configurable set of keywords and syntax."

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("disableSyntax", "DisableSyntax")(DisableSyntaxConfig.default)
      .map(DisableSyntax(_))

  private def checkRegex(doc: Doc): Seq[Diagnostic] = {
    def pos(offset: Int): Position =
      Position.Range(doc.input, offset, offset)
    val regexDiagnostics = Seq.newBuilder[Diagnostic]
    config.regex.foreach { regex =>
      val matcher = regex.value.matcher(doc.input.chars)
      val pattern = regex.value.pattern
      val message = regex.message.getOrElse(s"$pattern is disabled")
      while (matcher.find()) {
        regexDiagnostics +=
          Diagnostic(
            id = regex.id.getOrElse(pattern),
            message = message,
            position = pos(matcher.start)
          )
      }
    }
    regexDiagnostics.result()
  }

  private def checkTokens(doc: Doc): Seq[Diagnostic] = {
    doc.tree.tokens.collect {
      case token @ Keyword(keyword) if config.isDisabled(keyword) =>
        Diagnostic(s"keywords.$keyword", s"$keyword is disabled", token.pos)
      case token @ Token.Semicolon() if config.noSemicolons =>
        Diagnostic("noSemicolons", "semicolons are disabled", token.pos)
      case token @ Token.Tab() if config.noTabs =>
        Diagnostic("noTabs", "tabs are disabled", token.pos)
      case token @ Token.Xml.Start() if config.noXml =>
        Diagnostic("noXml", "xml literals are disabled", token.pos)
    }
  }

  private def checkTree(doc: Doc): Seq[Diagnostic] = {
    object AbstractWithVals {
      def unapply(t: Tree): Option[List[Defn.Val]] = {
        val stats = t match {
          case Defn.Class(mods, _, _, _, templ)
              if mods.exists(_.is[Mod.Abstract]) =>
            templ.stats
          case Defn.Trait(_, _, _, _, templ) => templ.stats
          case _ => List.empty
        }
        val vals = stats.flatMap {
          case v: Defn.Val => Some(v)
          case _ => None
        }
        if (vals.isEmpty) None else Some(vals)
      }
    }

    object DefaultArgs {
      def unapply(t: Tree): Option[List[Term]] = {
        t match {
          case d: Defn.Def => {
            val defaults =
              for {
                params <- d.paramss
                param <- params
                default <- param.default.toList
              } yield default

            Some(defaults)
          }
          case _ => None
        }
      }
    }

    object NoValPatterns {
      def unapply(t: Tree): Option[Tree] = t match {
        case v: Defn.Val =>
          v.pats.find(isProhibited)
        case v: Defn.Var =>
          v.pats.find(isProhibited)
        case _ => None
      }

      def isProhibited(v: Pat): Boolean = v match {
        case _: Pat.Tuple => false
        case _: Pat.Var => false
        case _ => true
      }
    }

    def hasNonImplicitParam(d: Defn.Def): Boolean =
      d.paramss.exists(_.exists(_.mods.forall(!_.is[Mod.Implicit])))

    val DefaultMatcher: PartialFunction[Tree, Seq[Diagnostic]] = {
      case Defn.Val(mods, _, _, _)
          if config.noFinalVal &&
            mods.exists(_.is[Mod.Final]) =>
        val mod = mods.find(_.is[Mod.Final]).get
        Seq(noFinalVal.at(mod.pos))
      case NoValPatterns(v) if config.noValPatterns =>
        Seq(noValPatternCategory.at(v.pos))
      case t @ mod"+" if config.noCovariantTypes =>
        Seq(
          Diagnostic(
            "covariant",
            "Covariant types could lead to error-prone situations.",
            t.pos
          )
        )
      case t @ mod"-" if config.noContravariantTypes =>
        Seq(
          Diagnostic(
            "contravariant",
            "Contravariant types could lead to error-prone situations.",
            t.pos
          )
        )
      case t @ AbstractWithVals(vals) if config.noValInAbstract =>
        vals.map { v =>
          Diagnostic(
            "valInAbstract",
            "val definitions in traits/abstract classes may cause initialization bugs",
            v.pos)
        }
      case t @ Defn.Object(mods, _, _)
          if mods.exists(_.is[Mod.Implicit]) && config.noImplicitObject =>
        Seq(
          Diagnostic(
            "implicitObject",
            "implicit objects may cause implicit resolution errors",
            t.pos)
        )
      case t @ Defn.Def(mods, _, _, paramss, _, _)
          if mods.exists(_.is[Mod.Implicit]) &&
            hasNonImplicitParam(t) &&
            config.noImplicitConversion =>
        Seq(
          Diagnostic(
            "implicitConversion",
            "implicit conversions weaken type safety and always can be replaced by explicit conversions",
            t.pos)
        )
      case DefaultArgs(params) if config.noDefaultArgs =>
        params
          .map { m =>
            Diagnostic(
              "defaultArgs",
              "Default args makes it hard to use methods as functions.",
              m.pos)
          }
    }
    val FinalizeMatcher = DisableSyntax.FinalizeMatcher("noFinalize")
    doc.tree.collect(DefaultMatcher.orElse(FinalizeMatcher)).flatten
  }

  override def fix(implicit doc: Doc): Patch = {
    (checkTree(doc) ++ checkTokens(doc) ++ checkRegex(doc))
      .map(Patch.lint)
      .asPatch
  }

  private val noFinalVal: LintCategory =
    LintCategory.error(
      id = "noFinalVal",
      explain = "Final vals cause problems with incremental compilation")
  private val noValPatternCategory: LintCategory =
    LintCategory.error(
      id = "noValPatterns",
      explain = "Pattern matching in val assignment can result in match error, " +
        "use \"_ match { ... }\" with a fallback case instead.")
}

object DisableSyntax {

  private val explanation =
    """|there is no guarantee that finalize will be called and
       |overriding finalize incurs a performance penalty""".stripMargin

  def FinalizeMatcher(id: String): PartialFunction[Tree, List[Diagnostic]] = {
    case Defn.Def(_, name @ q"finalize", _, Nil | Nil :: Nil, _, _) =>
      Diagnostic(
        id,
        "finalize should not be used",
        name.pos,
        explanation
      ) :: Nil
  }
}
