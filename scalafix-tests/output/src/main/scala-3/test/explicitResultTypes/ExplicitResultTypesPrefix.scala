
package test.explicitResultTypes

import java.nio.file.Paths

object ExplicitResultTypesPrefix {
  class Path
  def path: java.nio.file.Path = Paths.get("")
  object inner {
    val file: java.nio.file.Path = path
    object inner {
      val nio: java.nio.file.Path = path
      object inner {
        val java: _root_.java.nio.file.Path = path
      }
    }
  }
}
