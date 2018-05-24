package scalafix

import java.nio.file.Files
import scalafix.rule.ScalafixRules
import scalatags.Text
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import scalatags.Text.all._
import scala.meta._
import scala.meta.internal.io.PathIO

package object website {

  def url(name: String, relpath: String): Text.TypedTag[String] =
    a(href := s"/scalafix/$relpath", name)
  def ruleLink(name: String): Text.TypedTag[String] = {
    url(name, s"docs/rules/$name")
  }

  def allRulesTable: String = {
    val rules = ScalafixRules
      .all(SemanticdbIndex.empty)
      .filterNot(_.name.isDeprecated)
      .sortBy(_.name.value)

    val rows: List[Text.TypedTag[String]] = rules.map { rule =>
      val docPath = PathIO.workingDirectory
        .resolve("website")
        .resolve("src")
        .resolve("main")
        .resolve("tut")
        .resolve("docs")
        .resolve("rules")
        .resolve(rule.name.value + ".md")
      if (!Files.exists(docPath.toNIO)) {
        sys.error(
          s"Missing documentation for rule ${rule.name.value} in path $docPath")
      }
      val semantic = if (rule.isInstanceOf[SemanticRule]) "✅" else ""
      tr(
        td(semantic),
        td(ruleLink(rule.name.value)),
        td(rule.description)
      )
    }
    val html = table(
      thead(
        tr(
          th("Semantic"),
          th("Name"),
          th("Description")
        )
      ),
      tbody(rows)
    )
    html.toString()
  }

  // TODO(olafur) replace this hack with ConfEncoder[T] typeclass.
  def render(any: Any): String = any match {
    case s: Symbol =>
      val syntax =
        s.syntax.stripPrefix("_root_.").stripSuffix("#").stripSuffix(".")
      new StringBuilder()
        .append("\"")
        .append(syntax)
        .append("\"")
        .toString()
    case _ => any.toString
  }
  private def flat[T](default: T)(
      implicit settings: Settings[T],
      ev: T <:< Product): List[(Setting, Any)] = {
    settings.settings
      .zip(default.productIterator.toIterable)
      .flatMap {
        case (deepSetting, defaultSetting: Product)
            if deepSetting.underlying.nonEmpty =>
          deepSetting.flat.zip(defaultSetting.productIterator.toIterable)
        case (s, lst: Iterable[_]) =>
          val rendered = lst.map(render)
          val string =
            if (lst.size < 2) rendered.mkString("[", ", ", "]")
            else rendered.mkString("[\n  ", ",\n  ", "\n]")
          (s, string) :: Nil
        case (s, defaultValue) =>
          (s, defaultValue) :: Nil
      }

  }
  def htmlSetting(setting: Setting): Text.TypedTag[String] = {
    tr(
      td(code(setting.name)),
      td(
        // TODO(olafur) hack! Replace with ShowType[T] typeclass.
        setting.field.tpe
          .replace("scala.meta.Symbol.Global", "Symbol")
          .replace("java.util.regex.", "")
          .replace("scalafix.CustomMessage", "Message")
          .replace("scalafix.internal.config.", "")
      ),
      td(setting.description)
    )
  }

  def html(all: List[Setting]): String = {
    val fields = all.map { setting =>
      htmlSetting(setting)
    }
    table(
      thead(
        tr(
          th("Name"),
          th("Type"),
          th("Description")
        )
      ),
      tbody(fields)
    ).toString()
  }

  def config[T](name: String)(implicit settings: Settings[T]): String =
    s"#### $name\n\n" +
      html(settings.settings)

  def defaults[T](ruleName: String, default: T)(
      implicit settings: Settings[T],
      ev: T <:< Product): String =
    defaults[T](ruleName, flat(default))

  def defaults[T](ruleName: String, all: List[(Setting, Any)]): String = {
    val sb = new StringBuilder
    sb.append("#### Defaults\n\n```")
    all.foreach {
      case (setting, default) =>
        sb.append("\n")
          .append(ruleName)
          .append(".")
          .append(setting.name)
          .append(" = ")
          .append(default)
    }
    sb.append("\n```\n\n")
    sb.toString()
  }

  def examples[T](ruleName: String)(implicit settings: Settings[T]): String = {
    if (settings.settings.forall(_.exampleValues.isEmpty)) ""
    else {
      val sb = new StringBuilder
      sb.append("#### Examples\n\n```")
      settings.settings.foreach {
        case (setting) =>
          setting.exampleValues match {
            case Nil =>
            case example :: _ =>
              sb.append("\n")
                .append(ruleName)
                .append(".")
                .append(setting.name)
                .append(" = ")
                .append(example)
          }
      }
      sb.append("\n```\n\n")
      sb.toString()
    }
  }

  def rule[T](ruleName: String, default: T)(
      implicit settings: Settings[T],
      ev: T <:< Product): String = {
    val sb = new StringBuilder
    val all = flat(default)
    sb.append(html(all.map(_._1)))
    sb.append(defaults(ruleName, all))
    sb.append(examples[T](ruleName))
    sb.toString()
  }

}
