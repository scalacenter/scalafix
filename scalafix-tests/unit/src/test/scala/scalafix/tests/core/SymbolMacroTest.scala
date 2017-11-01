package scalafix.tests.core

import scala.meta._
import scalafix.internal.util.SymbolGlobal
import scalafix.testkit.ScalafixTest
import utest.compileError

object SymbolMacroTest extends ScalafixTest {

  test("compile OK") {
    val expected = Symbol("_root_.a.")
    assert(expected == SymbolGlobal("a"))
    assert(expected == SymbolGlobal("_root_.a"))
    assert(expected == SymbolGlobal("_root_.a."))
    assert(expected != SymbolGlobal("_root_.a#"))
  }

  test("compile error") {
    compileError("""SymbolGlobal("a.;b.")""")
    compileError("""SymbolGlobal(scala.compat.Platform.EOL)""")
    compileError("""SymbolGlobal("foo@1..2")""")
  }

}
