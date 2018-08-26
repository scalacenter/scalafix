package scalafix.v1

import java.net.URLClassLoader
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scalafix.internal.config.MetaconfigPendingUpstream._
import scalafix.internal.config._
import scalafix.internal.reflect.RuleDecoderOps.FromSourceRule
import scalafix.internal.reflect.RuleDecoderOps.tryClassload
import scalafix.internal.reflect.ScalafixToolbox
import scalafix.internal.reflect.ScalafixToolbox.CompiledRules
import scalafix.internal.v1.Rules
import scalafix.patch.TreePatch
import scalafix.v1
import scala.meta.io.Classpath
import scalafix.internal.reflect.ClasspathOps

/** One-stop shop for loading scalafix rules from strings. */
object RuleDecoder {

  /** Load a single rule from a string like "RemoveUnusedImports" or "file:path/to/Rule.scala"
    *
    * Supports loading rules in both scalafix.v0 and scalafix.v1.
    *
    * @param rule the name of the rule. See allowed syntax:
    *             https://scalacenter.github.io/scalafix/docs/users/configuration#rules
    * @param settings the settings for loading the rule.
    * @return a list of loaded rules, or errors.
    */
  def fromString(
      rule: String,
      allRules: List[v1.Rule],
      settings: Settings
  ): List[Configured[v1.Rule]] = {
    allRules.find(_.name.matches(rule)) match {
      case Some(r) =>
        Configured.ok(r) :: Nil
      case None =>
        fromStringURI(rule, settings.toolClasspath, settings)
    }
  }

  // Attempts to load a rule as if it was a URI, for example 'class:FQN' or 'github:org/repo/v1'
  private def fromStringURI(
      rule: String,
      classloader: URLClassLoader,
      settings: Settings
  ): List[Configured[v1.Rule]] = {
    val FromSource = new FromSourceRule(settings.cwd)
    Conf.Str(rule) match {
      // Patch.replaceSymbols(from, to)
      case UriRuleString("replace", replace @ SlashSeparated(from, to)) =>
        val constant = parseReplaceSymbol(from, to)
          .map(TreePatch.ReplaceSymbol.tupled)
          .map(p => scalafix.v1.SemanticRule.constant(replace, p))
        constant :: Nil
      // Classload rule from classloader
      case UriRuleString("scala" | "class", fqn) =>
        tryClassload(classloader, fqn) match {
          case Some(r) =>
            Configured.ok(r) :: Nil
          case _ =>
            ConfError.message(s"Class not found: $fqn").notOk :: Nil
        }
      // Compile rules from source with file/github/http protocols
      case FromSource(input) =>
        input match {
          case Configured.NotOk(err) => err.notOk :: Nil
          case Configured.Ok(code) =>
            ScalafixToolbox.getRule(code, settings.toolClasspath) match {
              case Configured.NotOk(err) => err.notOk :: Nil
              case Configured.Ok(CompiledRules(loader, names)) =>
                val x = names.iterator.map { fqn =>
                  tryClassload(loader, fqn) match {
                    case Some(r) =>
                      Configured.ok(r)
                    case _ =>
                      ConfError
                        .message(s"Failed to classload rule $fqn")
                        .notOk
                  }
                }.toList
                x
            }
        }
      case _ =>
        Configured.error(s"Unknown rule '$rule'") :: Nil
    }
  }

  def decoder(): ConfDecoder[Rules] =
    decoder(Settings())

  def decoder(settings: Settings): ConfDecoder[Rules] =
    new ConfDecoder[Rules] {
      private val allRules = Rules.all(settings.toolClasspath)
      override def read(conf: Conf): Configured[Rules] = conf match {
        case str: Conf.Str =>
          read(Conf.Lst(str :: Nil))
        case Conf.Lst(values) =>
          val decoded = values.flatMap {
            case Conf.Str(value) =>
              fromString(value, allRules, settings).map { rule =>
                rule.foreach(
                  _.name
                    .reportDeprecationWarning(value, settings.config.reporter))
                rule
              }
            case err =>
              ConfError.typeMismatch("String", err).notOk :: Nil
          }
          MetaconfigPendingUpstream.traverse(decoded).map { rules =>
            settings.config.patches.all match {
              case Nil => Rules(rules)
              case patches =>
                val hardcodedRule =
                  v1.SemanticRule.constant(".scalafix.conf", patches.asPatch)
                Rules(hardcodedRule :: rules)
            }
          }
        case els =>
          ConfError.typeMismatch("Either[String, List[String]]", els).notOk
      }
    }

  /**
    * Settings to load scalafix rules from configuration.
    *
    * To customize,
    *
    * {{{
    *   Settings().withConfig(...).withCwd(...)
    * }}}
    *
    * @param config the ScalafixConfig.
    * @param toolClasspath optional additional classpath entries for classloading/compiling
    *                      rules from classpath/source.
    * @param cwd the working directory to turn relative paths in file:Foo.scala into absolute paths.
    */
  final class Settings private (
      val config: ScalafixConfig,
      val toolClasspath: URLClassLoader,
      val cwd: AbsolutePath
  ) {

    def withConfig(value: ScalafixConfig): Settings = {
      copy(config = value)
    }

    def withToolClasspath(value: List[AbsolutePath]): Settings = {
      copy(toolClasspath = ClasspathOps.toClassLoader(Classpath(value)))
    }

    def withToolClasspath(value: URLClassLoader): Settings = {
      copy(toolClasspath = value)
    }

    def withCwd(value: AbsolutePath): Settings = {
      copy(cwd = value)
    }

    private def copy(
        config: ScalafixConfig = this.config,
        toolClasspath: URLClassLoader = this.toolClasspath,
        cwd: AbsolutePath = this.cwd
    ): Settings =
      new Settings(
        config,
        toolClasspath,
        cwd
      )
  }
  object Settings {
    def apply(): Settings =
      new Settings(
        ScalafixConfig.default,
        ClasspathOps.thisClassLoader,
        PathIO.workingDirectory
      )
  }

}
