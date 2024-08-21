package scalafix.internal.config

import scala.reflect.ClassTag

import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfError
import metaconfig.Configured

object ReaderUtil {
  // Poor mans coproduct reader
  def fromMap[T: ClassTag](
      m: Map[String, T],
      additionalMessage: PartialFunction[String, String] = PartialFunction.empty
  ): ConfDecoder[T] =
    ConfDecoder.instance[T] { case Conf.Str(x) =>
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
