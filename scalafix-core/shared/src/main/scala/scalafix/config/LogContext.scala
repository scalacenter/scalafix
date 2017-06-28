package scalafix.config

case class LogContext(line: sourcecode.Line,
                      file: sourcecode.File,
                      enclosing: sourcecode.Enclosing) {
  override def toString: String = enclosing.value.replaceAll(" .*", "")
}

object LogContext {
  implicit def generate(
      implicit line: sourcecode.Line,
      file: sourcecode.File,
      enclosing: sourcecode.Enclosing
  ) = LogContext(line, file, enclosing)
}
