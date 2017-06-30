package scalafix.internal.cli

sealed abstract class WriteMode {
  def isWriteFile: Boolean = this == WriteMode.WriteFile
}

object WriteMode {
  case object WriteFile extends WriteMode
  case object Stdout extends WriteMode
}
