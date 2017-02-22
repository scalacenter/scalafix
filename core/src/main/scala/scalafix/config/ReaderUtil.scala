package scalafix.config

import scala.reflect.ClassTag

import metaconfig.Reader

object ReaderUtil {
  def oneOf[T: ClassTag](options: sourcecode.Text[T]*): Reader[T] = {
    val m = options.map(x => x.source -> x.value).toMap
    fromMap(m)
  }

  // Poor mans coproduct reader
  def fromMap[T: ClassTag](m: Map[String, T]): Reader[T] =
    Reader.instance[T] {
      // Even if T takes type parameters.
      case x if implicitly[ClassTag[T]].runtimeClass.isInstance(x) =>
        Right(x.asInstanceOf[T])
      case x: String =>
        m.get(x) match {
          case Some(y) =>
            Right(y)
          case None =>
            val available = m.keys.mkString(", ")
            val msg =
              s"Unknown input '$x'. Expected one of $available"
            Left(new IllegalArgumentException(msg))
        }
    }
}
