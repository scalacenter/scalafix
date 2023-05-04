package scalafix.internal.interfaces

import scalafix.interfaces.ScalafixPosition
import scalafix.interfaces.ScalafixTextEdit

final case class ScalafixTextEditImpl(
    position: ScalafixPosition,
    newText: String
) extends ScalafixTextEdit
