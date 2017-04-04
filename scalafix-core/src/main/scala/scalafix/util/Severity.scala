package scalafix.util

import scalafix.config.ReaderUtil

import metaconfig.Reader

sealed abstract class Severity(color: String, val order: Int)(
    implicit name: sourcecode.Name)
    extends Ordered[Severity] {
  override val toString: String =
    color + name.value.toLowerCase + Console.RESET
  override def compare(that: Severity): Int = this.order - that.order
}

object Severity {
  case object Trace extends Severity(Console.RESET, 1)
  case object Debug extends Severity(Console.GREEN, 2)
  case object Info extends Severity(Console.BLUE, 3)
  case object Warn extends Severity(Console.YELLOW, 4)
  case object Error extends Severity(Console.RED, 5)
  implicit val SeverityReader: Reader[Severity] =
    ReaderUtil.oneOf[Severity](Trace, Debug, Info, Warn, Error)
}
