package scalafix.internal.cli

import metaconfig._
import metaconfig.generic.Setting
import metaconfig.generic.Settings
import metaconfig.Configured.ok
import metaconfig.internal.Case
import scala.annotation.StaticAnnotation

object CliParser {

  class repeated extends StaticAnnotation

  def parseArgs[T](args: List[String])(
      implicit settings: Settings[T]): Configured[Conf] = {
    def loop(
        curr: Conf.Obj,
        xs: List[String],
        s: State): Configured[Conf.Obj] = {
      def add(key: String, value: Conf) = Conf.Obj((key, value) :: curr.values)
      (xs, s) match {
        case (Nil, NoFlag) => ok(curr)
        case (Nil, Flag(flag, _)) => ok(add(flag, Conf.fromBoolean(true)))
        case (head :: tail, NoFlag) =>
          if (head.startsWith("-")) {
            val camel = Case.kebabToCamel(dash.replaceFirstIn(head, ""))
            camel.split("\\.").toList match {
              case Nil =>
                ConfError.message(s"Flag '$head' must not be empty").notOk
              case flag :: flags =>
                settings.get(flag, flags) match {
                  case None =>
                    settings.get(flag) match {
                      // TODO: upstream special handling for metaconfig.Conf fields.
                      case Some(setting) if setting.tpe == "metaconfig.Conf" =>
                        loop(curr, tail, Flag(camel, setting))
                      case _ =>
                        ConfError
                          .invalidFields(camel :: Nil, settings.names)
                          .notOk
                    }
                  case Some(setting) =>
                    if (setting.isBoolean) {
                      val newCurr = add(camel, Conf.fromBoolean(true))
                      loop(newCurr, tail, NoFlag)
                    } else {
                      loop(curr, tail, Flag(camel, setting))
                    }
                }
            }
          } else {
            ok(add("remainingArgs", Conf.fromList(xs.map(Conf.fromString))))
          }
        case (head :: tail, Flag(flag, setting)) =>
          val value = Conf.fromNumberOrString(head)
          val newCurr =
            if (setting.isRepeated) {
              curr.map.get(flag) match {
                case Some(Conf.Lst(values)) => Conf.Lst(values :+ value)
                case _ => Conf.Lst(value :: Nil)
              }
            } else {
              value
            }
          loop(add(flag, newCurr), tail, NoFlag)
      }
    }
    loop(Conf.Obj(), args, NoFlag).map(_.normalize)
  }

  private sealed trait State
  private case class Flag(flag: String, setting: Setting) extends State
  private case object NoFlag extends State
  private val dash = "--?".r

  implicit class XtensionSetting(setting: Setting) {
    def isRepeated: Boolean = {
      // see https://github.com/olafurpg/metaconfig/issues/50
      setting.tpe.contains("List[") ||
      setting.annotations.exists(_.isInstanceOf[repeated])
    }
  }

}
