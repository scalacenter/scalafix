package scalafix.internal.rule

case class CompilerException(underlying: Throwable)
    extends Exception("compiler crashed", underlying)
