package scalafix.internal.v1

import metaconfig.{Conf, ConfOps}
import metaconfig.Conf.Obj

object ScalafixConfOps {
  def drop(original: Conf, key: String): Conf =
    ConfOps.fold(original)(obj = { confObj =>
      Obj(confObj.values.filterNot(_._1 == key))
    })
}
