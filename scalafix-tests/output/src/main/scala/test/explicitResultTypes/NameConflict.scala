package test.explicitResultTypes

import scala.reflect.io.File
object NameConflict {
  def a: File = null.asInstanceOf[scala.reflect.io.File]
  def b: java.io.File = null.asInstanceOf[java.io.File]
}