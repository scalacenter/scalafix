package scalafix.internal.rule

import metaconfig.Configured
import java.util.regex.Matcher
import scala.meta._
import scalafix.v0.LintCategory
import scalafix.v1._

final class DisableSyntax(config: DisableSyntaxConfig)
    extends SyntacticRule("DisableSyntax") {

  def this() = this(DisableSyntaxConfig())

  override def description: String =
    "Reports an error for disabled features such as var or XML literals."
  override def isLinter: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("disableSyntax", "DisableSyntax")(DisableSyntaxConfig.default)
      .map(new DisableSyntax(_))

  private def checkRegex(doc: SyntacticDocument): Seq[Diagnostic] = {
    def pos(offset: Int): Position =
      Position.Range(doc.input, offset, offset)

    def messageSubstitution(matcher: Matcher, message: String): String =
      (0 to matcher.groupCount).foldLeft(message) {
        case (msg, idx) => msg.replaceAll("$"+idx, matcher.group(idx))
      }

    val regexDiagnostics = Seq.newBuilder[Diagnostic]
    config.regex.foreach { regex =>
      val (matcher, pattern, groupIndex) = regex.value match {
        case Left(pat) => (pat.matcher(doc.input.chars), pat.pattern, 0)
        case Right(reg) =>
          val pattern = reg.pattern
          val groupIndex = reg.captureGroup.getOrElse(0)
          (pattern.matcher(doc.input.chars), pattern.pattern, groupIndex)
      }

      val message = regex.message.getOrElse(s"$pattern is disabled")
      while (matcher.find()) {
        regexDiagnostics +=
          Diagnostic(
            id = regex.id.getOrElse(pattern),
            message = messageSubstitution(matcher, message),
            position = pos(matcher.start(groupIndex))
          )
      }
    }
    regexDiagnostics.result()
  }

  private def checkTokens(doc: SyntacticDocument): Seq[Diagnostic] = {
    doc.tree.tokens.collect {
      case token @ Keyword(keyword) if config.isDisabled(keyword) =>
        keyword match {
          case "var" =>
            Diagnostic(keyword, s"mutable state should be avoided", token.pos)
          case "null" =>
            Diagnostic(
              keyword,
              "null should be avoided, consider using Option instead",
              token.pos)
          case "throw" =>
            Diagnostic(
              keyword,
              "exceptions should be avoided, consider encoding the error in the return type instead",
              token.pos)
          case "return" =>
            Diagnostic(
              keyword,
              "return should be avoided, consider using if/else instead",
              token.pos)
          case "while" =>
            Diagnostic(
              keyword,
              "while loops should be avoided, consider using recursion instead",
              token.pos)
          case _ =>
            Diagnostic(keyword, s"$keyword is disabled", token.pos)
        }
      case token @ Token.Semicolon() if config.noSemicolons =>
        Diagnostic("noSemicolons", "semicolons are disabled", token.pos)
      case token @ Token.Tab() if config.noTabs =>
        Diagnostic("noTabs", "tabs are disabled", token.pos)
      case token @ Token.Xml.Start() if config.noXml =>
        Diagnostic("noXml", "xml literals should be avoided", token.pos)
      case token: Token.Ident
          if token.value == "asInstanceOf" && config.noAsInstanceOf =>
        Diagnostic(
          "asInstanceOf",
          "asInstanceOf casts are disabled, use pattern matching instead",
          token.pos)
      case token: Token.Ident
          if token.value == "isInstanceOf" && config.noIsInstanceOf =>
        Diagnostic(
          "isInstanceOf",
          "isInstanceOf checks are disabled, use pattern matching instead",
          token.pos)
    }
  }

  private def checkTree(doc: SyntacticDocument): Seq[Diagnostic] = {
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
      case Term.ApplyInfix(_, t @ Term.Name("=="), _, _)
          if config.noUniversalEquality =>
        Seq(noUniversalEqualityDiagnostic(t))
      case Term.Apply(Term.Select(_, t @ Term.Name("==")), _)
          if config.noUniversalEquality =>
        Seq(noUniversalEqualityDiagnostic(t))
    }
    val FinalizeMatcher = DisableSyntax.FinalizeMatcher("noFinalize")
    doc.tree.collect(DefaultMatcher.orElse(FinalizeMatcher)).flatten
  }

  override def fix(implicit doc: SyntacticDocument): Patch = {
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

  private def noUniversalEqualityDiagnostic(t: Term.Name): Diagnostic =
    Diagnostic("==", config.noUniversalEqualityMessage, t.pos)

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
