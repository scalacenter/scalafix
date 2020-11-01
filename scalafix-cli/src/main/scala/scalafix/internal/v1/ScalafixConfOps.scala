package scalafix.internal.v1

import metaconfig.Conf
import metaconfig.Conf.Obj
import metaconfig.ConfOps
import metaconfig.internal.ConfGet

object ScalafixConfOps {
  def drop(original: Conf, key: String): Conf =
    ConfOps.fold(original)(obj = { confObj =>
      Obj(confObj.values.filterNot(_._1 == key))
    })

  def overlay(original: Conf, key: String): Conf = {
    val child = ConfGet.getKey(original, key :: Nil)
    child.fold(original)(
      ConfOps.merge(drop(original, key), _)
    )
  }
}
