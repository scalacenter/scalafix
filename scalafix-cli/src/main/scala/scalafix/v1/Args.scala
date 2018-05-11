package scalafix.v1

import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import metaconfig._
import metaconfig.generic.Surface
import metaconfig.internal.ConfGet
import org.langmeta.internal.io.FileIO
import scala.meta.Input
import scala.meta.Source
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.meta.parsers.Parsed
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.cli.WriteMode
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules
import scalafix.reflect.ScalafixReflectV1
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser

case class ValidatedArgs(
    args: Args,
    symtab: SymbolTable,
    rules: Rules
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

  def matches(path: AbsolutePath): Boolean =
    Args.baseMatcher.matches(path.toNIO) &&
      files.forall(_.matches(path.toNIO))

}

case class Args(
    rules: List[String] = Nil,
    config: List[AbsolutePath] = Nil,
    classpath: Classpath = Classpath(Nil),
    ls: Ls = Ls.Find,
    cwd: AbsolutePath = PathIO.workingDirectory,
    sourceroot: AbsolutePath = PathIO.workingDirectory,
    files: List[PathMatcher] = Nil,
    out: PrintStream = System.out,
    parser: MetaParser = MetaParser(),
    charset: Charset = StandardCharsets.UTF_8,
    stdout: Boolean = false,
    test: Boolean = false,
    metacpCacheDir: List[AbsolutePath] = Nil,
    metacpParallel: Boolean = false
) {

  def configuredSymtab: Configured[SymbolTable] = {
    ClasspathOps.newSymbolTable(
      classpath = classpath,
      cacheDirectory = metacpCacheDir.headOption,
      parallel = metacpParallel,
      out = out
    ) match {
      case Some(symtab) =>
        Configured.ok(symtab)
      case _ =>
        ConfError.message("Unable to load symbol table").notOk
    }
  }

  def configuredRules: Configured[Rules] = {
    val rulesConf = Conf.Lst(rules.map(Conf.fromString))
    config match {
      case Nil => ScalafixReflectV1.decoder.read(rulesConf)
      case file :: _ =>
        val input = metaconfig.Input.File(file.toNIO)
        Conf.parseInput(input).andThen { fileConf =>
          val finalRules: Configured[Conf] =
            if (rules.isEmpty) {
              ConfGet.getKey(fileConf, "rules" :: "rule" :: Nil) match {
                case Some(c) => Configured.ok(c)
                case _ => ConfError.message("No rule provided").notOk
              }
            } else {
              Configured.ok(rulesConf)
            }
          finalRules.andThen { rulesConf =>
            ScalafixReflectV1.decoder
              .read(rulesConf)
              .andThen(_.withConfig(fileConf))
          }
        }
    }
  }

  def validate: Configured[ValidatedArgs] = {
    (configuredSymtab |@| configuredRules).map {
      case (symtab, rulez) =>
        ValidatedArgs(this, symtab, rulez)
    }
  }
}

object Args {
  val baseMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt}")

  implicit val surface: Surface[Args] = generic.deriveSurface
  implicit val decoder: ConfDecoder[Args] = generic.deriveDecoder(Args())

  implicit val charsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.map(name => Charset.forName(name))
  implicit val classpathDecoder: ConfDecoder[Classpath] =
    ConfDecoder.stringConfDecoder.map(Classpath(_))
  implicit val absolutePathDecoder: ConfDecoder[AbsolutePath] =
    ConfDecoder.stringConfDecoder.map(AbsolutePath(_))
  implicit val printStreamDecoder: ConfDecoder[PrintStream] =
    ConfDecoder.stringConfDecoder.map(_ => System.out)
  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob =>
      FileSystems.getDefault.getPathMatcher("glob:" + glob))
}
