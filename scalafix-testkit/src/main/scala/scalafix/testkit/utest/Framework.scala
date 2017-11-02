package scalafix.testkit.utest

import _root_.utest.runner

class Framework extends runner.Framework {
  override def exceptionStackFrameHighlighter(s: StackTraceElement): Boolean = {
    s.getClassName.contains("scalafix.")
  }
}
