package scalafix.internal.cli

import java.io.File
import scala.meta.Input

// a file that is supposed to be run through scalafix.
case class FixFile(
    // The file on the local filesystem where the fix should written to.
    original: Input.File,
    // For semantic rewrites on fat semanticdb, the input in scala.meta.Database
    // is labeled strings instead of Input.File. The labeled string must be used
    // in the RewriteCtx in order to position lookups in Database.names/symbols
    // to match, since scala.meta.Position.input must match.
    mirror: Option[Input.LabeledString] = None,
    // Was this file passed explicitly or expanded from a directory?
    // If the file was expanded from a directory, we may want to skip reporting
    // a parse error.
    passedExplicitly: Boolean = false
) {
  override def toString: String =
    s"InputFile(${original.path.toNIO}, $mirror, $passedExplicitly)"
  def toParse: Input = mirror.getOrElse(original)
  def toIO: File = original.path.toFile
}
