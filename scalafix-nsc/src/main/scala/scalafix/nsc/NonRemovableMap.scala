package scalafix.nsc

import scala.collection.mutable
import scala.util.Try

/** Hack to get used symbols in compilation unit */
class NonRemovableMap[K, V](default: V) extends mutable.HashMap[K, V] {
  override def default(key: K): V = default
  def customRemove(key: AnyRef): Option[V] =
    Try(super.remove(key.asInstanceOf[K])).toOption.flatten

  // do nothing, see custom remove
  override def remove(key: K): Option[V] = get(key)
  override def -=(key: K): this.type = this
}
