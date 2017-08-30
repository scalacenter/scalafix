package scalafix.tests

import scalafix.internal.util.SymbolGlobal
import scala.meta._
import org.scalatest.FunSuite

class SymbolMacroTest extends FunSuite {

  test("compile OK") {
    val expected = Symbol("_root_.a.")
    assert(expected == SymbolGlobal("a"))
    assert(expected == SymbolGlobal("_root_.a"))
    assert(expected == SymbolGlobal("_root_.a."))
    assert(expected != SymbolGlobal("_root_.a#"))
  }

  test("compile error") {
    assertCompiles("""SymbolGlobal("a")""")
    assertDoesNotCompile("""SymbolGlobal("a.;b.")""")
    assertDoesNotCompile("""SymbolGlobal(scala.compat.Platform.EOL)""")
    assertDoesNotCompile("""SymbolGlobal("foo@1..2")""")
  }

}
