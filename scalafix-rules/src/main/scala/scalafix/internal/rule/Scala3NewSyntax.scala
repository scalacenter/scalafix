package scalafix.internal.rule

import scala.meta._

import metaconfig.ConfDecoder
import metaconfig.Configured
import metaconfig.annotation.Description
import metaconfig.generic
import metaconfig.generic.Surface
import scalafix.v1._

final class Scala3NewSyntax(config: Scala3NewSyntaxConfig)
    extends SemanticRule("Scala3NewSyntax") {

  def this() = this(Scala3NewSyntaxConfig.default)

  override def isRewrite: Boolean = true

  override def isLinter: Boolean = true

  override def withConfiguration(config: Configuration): Configured[Rule] = {
    if (config.scalaVersion.nonEmpty && config.scalaVersion.startsWith("3")) {
      config.conf
        .getOrElse("Scala3NewSyntax", "Scala3NewSyntax")(
          Scala3NewSyntaxConfig.default
        )
        .map(new Scala3NewSyntax(_))
    } else
      Configured.error(
        s"${this.name} can run only on Scala 3, not in ${config.scalaVersion}"
      )

  }

  override def description: String =
    "Apply a number of rewrite that are specific to Scala 3."

  override def fix(implicit doc: SemanticDocument): Patch = {

    val rewritePatch = doc.tree.collect {
      case t: Importee.Wildcard
          if t.tokens.head.is[Token.Underscore] && config.newSyntax =>
        Patch.replaceTree(t, "*")
      case t: Type.Placeholder
          if t.tokens.toList.exists(
            _.is[Token.Underscore]
          ) =>
        val underscore = t.tokens.find(_.is[Token.Underscore]).get
        Patch.replaceToken(underscore, "?")
    }.asPatch

    val lintPatch = doc.tokens.collect { case t @ Token.KwImplicit() =>
      Patch.lint(
        Diagnostic(
          "Implicits",
          "Implicit can be replace with using/given",
          t.pos
        )
      )
    }.asPatch

    rewritePatch + lintPatch
  }
  def sanitizeTemplate(s: Template): String = {
    val lines = s.toString().linesIterator.toList
    val firstline = lines.head.replaceAll("[:{]", "")
    val lastLine = lines.last.replaceAll("[}]", "")
    (firstline +:
      lines.drop(1).dropRight(1).map(_.replaceAll("^  ", "")) :+
      lastLine).mkString("\n")
  }

}
case class Scala3NewSyntaxConfig(
    @Description("if true, it will rewrite stuff")
    newSyntax: Boolean = true,
    dropPackageObject: Boolean = true
)
object Scala3NewSyntaxConfig {
  val default: Scala3NewSyntaxConfig = Scala3NewSyntaxConfig()
  implicit val surface: Surface[Scala3NewSyntaxConfig] =
    generic.deriveSurface[Scala3NewSyntaxConfig]
  // Do not remove this import even if IntelliJ thinks it's unused
  import scalafix.internal.config.ScalafixMetaconfigReaders._ // scalafix:ok
  implicit val decoder: ConfDecoder[Scala3NewSyntaxConfig] =
    generic.deriveDecoder[Scala3NewSyntaxConfig](default)
}
