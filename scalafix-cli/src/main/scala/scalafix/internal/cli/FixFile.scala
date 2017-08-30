package scalafix.internal.cli

import java.io.File
import scala.meta.Input

// a file that is supposed to be run through scalafix.
case class FixFile(
    // The file on the local filesystem where the fix should written to.
    original: Input.File,
    // For semantic rewrites on fat semanticdb, the input in scalafix.SemanticCtx
    // is labeled strings instead of Input.File. The labeled string must be used
    // in the RewriteCtx in order to position lookups in SemanticCtx.names/symbols
    // to match, since scala.meta.Position.input must match.
    sctx: Option[Input.VirtualFile] = None,
    // Was this file passed explicitly or expanded from a directory?
    // If the file was expanded from a directory, we may want to skip reporting
    // a parse error.
    passedExplicitly: Boolean = false
) {
  override def toString: String =
    s"InputFile(${original.path.toNIO}, $sctx, $passedExplicitly)"
  def toParse: Input = sctx.getOrElse(original)
  def toIO: File = original.path.toFile
}
