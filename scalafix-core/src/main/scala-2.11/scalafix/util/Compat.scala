package scalafix.util

import scala.collection.immutable.IndexedSeq

object Compat {
  type View[T] = collection.SeqView[T, IndexedSeq[T]]
  type SeqView[T] = collection.SeqView[T, IndexedSeq[T]]

}
