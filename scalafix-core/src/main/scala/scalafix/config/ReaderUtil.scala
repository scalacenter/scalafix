package scalafix.config

import scala.reflect.ClassTag

import metaconfig.ConfDecoder
import metaconfig.Conf
import metaconfig.ConfError
import metaconfig.Configured

object ReaderUtil {
  def oneOf[T: ClassTag](options: sourcecode.Text[T]*): ConfDecoder[T] = {
    val m = options.map(x => x.source -> x.value).toMap
    fromMap(m)
  }

  // Poor mans coproduct reader
  def fromMap[T: ClassTag](m: Map[String, T]): ConfDecoder[T] =
    ConfDecoder.instance[T] {
      case Conf.Str(x) =>
        m.get(x) match {
          case Some(y) =>
            Configured.Ok(y)
          case None =>
            val available = m.keys.mkString(", ")
            val msg = s"Unknown input '$x'. Expected one of: $available"
            Configured.NotOk(ConfError.msg(msg))
        }
    }
}
