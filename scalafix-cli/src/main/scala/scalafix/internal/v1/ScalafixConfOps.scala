package scalafix.internal.v1

import metaconfig.{Conf, ConfOps}
import metaconfig.Conf.Obj

object ScalafixConfOps {
  def mergeShallow(primary: Conf, secondary: Conf): Conf =
    mergeShallow(primary, Option(secondary))

  def mergeShallow(primary: Conf, secondary: Option[Conf]): Conf =
    (primary, secondary) match {
      case (Obj(v1), Some(Obj(v2))) =>
        val merged = v1 ++ v2.filterNot {
          case (key, _) => v1.exists(_._1 == key)
        }
        Obj(merged)

      case (_, _) => primary
    }

  def drop(original: Conf, key: String): Conf =
    ConfOps.fold(original)(obj = { confObj =>
      Obj(confObj.values.filterNot(_._1 == key))
    })
}
