package scalafix.v1

import org.langmeta.internal.io.FileIO
import org.langmeta.io.RelativePath
import scala.meta.Input
import scala.meta.AbsolutePath
import scala.meta.Classpath
import scala.meta.Source
import scala.meta.parsers.Parsed
import scalafix.internal.cli.WriteMode
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules

case class ValidatedArgs(
    args: Args,
    symtab: SymbolTable,
    rules: Rules,
    config: ScalafixConfig,
    classpath: Classpath,
    sourceroot: AbsolutePath,
    pathReplace: AbsolutePath => AbsolutePath
) {
  import args._

  val mode: WriteMode =
    // TODO: suppress
    if (stdout) WriteMode.Stdout
    else if (test) WriteMode.Test
    else WriteMode.WriteFile

  def input(file: AbsolutePath): Input = {
    Input.VirtualFile(file.toString(), FileIO.slurp(file, charset))
  }

  def parse(input: Input): Parsed[Source] = {
    import scala.meta._
    val dialect = parser.dialectForFile(input.syntax)
    dialect(input).parse[Source]
  }

  def matches(path: RelativePath): Boolean =
    Args.baseMatcher.matches(path.toNIO) && {
      args.exclude.forall(!_.matches(path.toNIO))
    }

}
