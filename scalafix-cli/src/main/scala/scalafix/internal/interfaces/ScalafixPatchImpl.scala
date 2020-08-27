package scalafix.internal.interfaces

import scalafix.interfaces.ScalafixPatch
import scalafix.Patch

case class ScalafixPatchImpl(patch: Patch) extends ScalafixPatch
