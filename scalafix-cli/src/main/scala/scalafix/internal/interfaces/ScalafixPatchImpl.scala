package scalafix.internal.interfaces

import scalafix.Patch
import scalafix.interfaces.ScalafixPatch

case class ScalafixPatchImpl(patch: Patch) extends ScalafixPatch
