package scalafix.util

object Compat {
  type View[T] = collection.View[T]
  type SeqView[T] = collection.SeqView[T]

}
