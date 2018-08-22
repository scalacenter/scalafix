package scalafix.v1

class MissingSymbolException(val sym: Symbol)
    extends Exception(s"Missing symbol $sym")
