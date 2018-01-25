package scalafix

import scalatags.Text
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import scalatags.Text.all._

package object website {
  private def flat[T](default: T)(
      implicit settings: Settings[T],
      ev: T <:< Product): List[(Setting, Any)] = {
    settings.settings
      .zip(default.productIterator.toIterable)
      .flatMap {
        case (deepSetting, defaultSetting: Product)
            if deepSetting.underlying.nonEmpty =>
          deepSetting.flat.zip(defaultSetting.productIterator.toIterable)
        case (s, lst: List[_]) =>
          (s, lst.mkString("[", ", ", "]")) :: Nil
        case (s, defaultValue) =>
          (s, defaultValue) :: Nil
      }

  }
  def htmlSetting(setting: Setting): Text.TypedTag[String] = {
    tr(
      td(code(setting.name)),
      // TODO(olafur) hack!
      td(setting.field.tpe.replace("scalafix.internal.config.", "")),
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
