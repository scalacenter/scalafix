package scalafix.internal.v1

import scala.meta.internal.io.FileIO
import scala.meta.io.RelativePath
import scala.meta.Input
import scala.meta.AbsolutePath
import scala.meta.Source
import scala.meta.parsers.Parsed
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.diff.DiffDisable
import scala.meta.internal.symtab.SymbolTable

case class ValidatedArgs(
    args: Args,
    symtab: SymbolTable,
    rules: Rules,
    config: ScalafixConfig,
    classLoader: ClassLoader,
    sourceroot: AbsolutePath,
    pathReplace: AbsolutePath => AbsolutePath,
    diffDisable: DiffDisable,
    callback: DelegatingMainCallback
) {

  def input(file: AbsolutePath): Input = {
    Input.VirtualFile(file.toString(), FileIO.slurp(file, args.charset))
  }

  def parse(input: Input): Parsed[Source] = {
    import scala.meta._
    val dialect = config.parser.dialectForFile(input.syntax)
    dialect(input).parse[Source]
  }

  def matches(path: RelativePath): Boolean =
    Args.baseMatcher.matches(path.toNIO) && {
      args.exclude.forall(!_.matches(path.toNIO))
    }

}
