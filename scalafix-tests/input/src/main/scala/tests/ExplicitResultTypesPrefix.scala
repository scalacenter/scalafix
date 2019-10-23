/*
rules = "ExplicitResultTypes"
 */
package tests

import java.nio.file.Paths

object ExplicitResultTypesPrefix {
  class Path
  def path = Paths.get("")
  object inner {
    val file = path
    object inner {
      val nio: java.nio.file.Path = path
      object inner {
        val java = path
      }
    }
  }
}