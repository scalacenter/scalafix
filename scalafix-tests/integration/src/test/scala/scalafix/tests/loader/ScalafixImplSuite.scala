package scalafix.tests.loader

import org.scalatest.funsuite.AnyFunSuite
import scalafix.Versions
import scalafix.interfaces.Scalafix

class ScalafixImplSuite extends AnyFunSuite {

  test("the scalafix version is exposed") {
    assert(Scalafix.get().scalafixVersion == Versions.version)
  }

  // TODO: mainHelp

}
