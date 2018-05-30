package scalafix.v1

class MissingSymbolException(val sym: Sym)
    extends Exception(s"Missing symbol $sym")
