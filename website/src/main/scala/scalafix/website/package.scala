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
          pprint.log(deepSetting)
          deepSetting.flat.zip(defaultSetting.productIterator.toIterable)
        case (s, lst: List[_]) =>
          (s, lst.mkString("[", ", ", "]")) :: Nil
        case (s, defaultValue) =>
          (s, defaultValue) :: Nil
      }

  }

  def rule[T](ruleName: String, default: T)(
      implicit settings: Settings[T],
      ev: T <:< Product): String = {
    val sb = new StringBuilder
    val all = flat(default)
    def htmlSetting(
        setting: Setting,
        defaultValue: Any): Text.TypedTag[String] = {
      tr(
        td(code(setting.name)),
        // TODO(olafur) hack!
        td(setting.field.tpe.replace("scalafix.internal.config.", "")),
        td(setting.description)
      )
    }

    def html: String = {
      val fields = all.map {
        case (setting, defaultValue) =>
          htmlSetting(setting, defaultValue)
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
    sb.append(html)
    sb.append("#### Defaults\n\n```")
    all.foreach {
      case (setting, value) =>
        sb.append("\n")
          .append(ruleName)
          .append(".")
          .append(setting.name)
          .append(" = ")
          .append(value)

        setting.exampleValues match {
          case Nil =>
          case example :: _ =>
            if (setting.exampleValues.nonEmpty) {
              sb.append(" // ")
                .append("Example: ")
                .append(example)
            }
        }
    }
    sb.append("\n```\n\n")
    sb.toString()
  }

}
