package scalafix.internal.config

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
  def fromMap[T: ClassTag](
      m: Map[String, T],
      additionalMessage: PartialFunction[String, String] = PartialFunction.empty
  ): ConfDecoder[T] =
    ConfDecoder.instance[T] {
      case Conf.Str(x) =>
        m.get(x) match {
          case Some(y) =>
            Configured.Ok(y)
          case None =>
            val available = m.keys.mkString(", ")
            val extraMsg = additionalMessage.applyOrElse(x, (_: String) => "")
            val msg =
              s"Unknown input '$x'. Expected one of: $available. $extraMsg"
            Configured.NotOk(ConfError.message(msg))
        }
    }
}
