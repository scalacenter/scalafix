package scalafix.internal.interfaces

import java.nio.CharBuffer
import java.nio.file.Path
import java.util.Optional
import scala.meta.inputs.Input
import scala.{meta => m}
import scalafix.interfaces.ScalafixInput

object ScalafixInputImpl {
  def fromScala(input: m.Input): ScalafixInput =
    new ScalafixInput {
      override def text(): CharSequence = input match {
        case Input.VirtualFile(_, value) => value
        case _ => CharBuffer.wrap(input.chars)
      }
      override def filename(): String = input.syntax
      override def path(): Optional[Path] = input match {
        case Input.File(path, _) => Optional.of(path.toNIO)
        case _ => Optional.empty()
      }
    }
}
