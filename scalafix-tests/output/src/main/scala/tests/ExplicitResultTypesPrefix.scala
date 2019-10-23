package tests

import java.nio.file.Paths
import java.nio
import java.nio.file

object ExplicitResultTypesPrefix {
  class Path
  def path: file.Path = Paths.get("")
  object inner {
    val file: nio.file.Path = path
    object inner {
      val nio: java.nio.file.Path = path
      object inner {
        val java: _root_.java.nio.file.Path = path
      }
    }
  }
}