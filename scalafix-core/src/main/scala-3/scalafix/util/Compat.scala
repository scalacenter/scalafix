package scalafix.util

object Compat {
  type View[T] = collection.SeqView[T]
  type SeqView[T] = collection.SeqView[T]

}
