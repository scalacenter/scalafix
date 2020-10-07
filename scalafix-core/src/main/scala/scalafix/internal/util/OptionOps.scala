package scalafix.internal.util

import java.util.Optional

import scala.util.Success
import scala.util.Try
import scala.util.{Failure => ScalaFailure}

object OptionOps {
  implicit class OptionExtension[A](val in: Option[A]) extends AnyVal {
    def asJava: Optional[A] = in match {
      case Some(a) => Optional.ofNullable(a)
      case _ => Optional.empty[A]
    }
    def toTry: Try[A] = in match {
      case Some(v) => Success(v)
      case None => ScalaFailure(new Exception("Empty value"))
    }
  }

  implicit class OptionalExtension[A](val in: Optional[A]) extends AnyVal {
    def asScala: Option[A] = if (in.isPresent) Some(in.get()) else None
  }
}
