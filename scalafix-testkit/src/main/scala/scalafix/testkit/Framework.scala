package scalafix.testkit

class Framework extends utest.runner.Framework {
  override def exceptionStackFrameHighlighter(s: StackTraceElement): Boolean = {
    s.getClassName.contains("scalafix.")
  }
}
