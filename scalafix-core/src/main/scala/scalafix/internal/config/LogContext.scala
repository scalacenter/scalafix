package scalafix.internal.config

case class LogContext(
    line: sourcecode.Line,
    file: sourcecode.File,
    enclosing: sourcecode.Enclosing) {
  override def toString: String = enclosing.value.replaceAll(" .*", "")
}

object LogContext {
  implicit def generate(
      implicit line: sourcecode.Line,
      file: sourcecode.File,
      enclosing: sourcecode.Enclosing
  ): LogContext = LogContext(line, file, enclosing)
}
