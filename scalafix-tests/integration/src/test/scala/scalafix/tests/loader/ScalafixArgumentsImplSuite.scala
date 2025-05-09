package scalafix.tests.loader

import org.scalatest.funsuite.AnyFunSuite
import scalafix.internal.interfaces.ScalafixArgumentsImpl

class ScalafixArgumentsImplSuite extends AnyFunSuite {

  test("availableRules") {
    new ScalafixArgumentsImpl().availableRules()
  }

}
