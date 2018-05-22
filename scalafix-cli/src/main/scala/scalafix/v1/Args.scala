package scalafix.v1

import java.io.PrintStream
import java.net.URI
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import metaconfig.Configured._
import metaconfig._
import metaconfig.annotation.ExtraName
import metaconfig.generic.Surface
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import org.langmeta.internal.io.FileIO
import org.langmeta.io.RelativePath
import scala.meta.Input
import scala.meta.Source
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.meta.parsers.Parsed
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.cli.WriteMode
import scalafix.internal.config.OutputFormat
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.util.ClassloadRule
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules
import scalafix.reflect.ScalafixReflectV1

case class ValidatedArgs(
    args: Args,
    symtab: SymbolTable,
    rules: Rules,
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
    Args.baseMatcher.matches(path.toNIO) &&
      files.forall(_.matches(path.toNIO)) &&
      args.exclude.forall(!_.matches(path.toNIO))

}

case class Args(
    @ExtraName("r")
    rules: List[String] = Nil,
    config: List[AbsolutePath] = Nil,
    toolClasspath: List[AbsolutePath] = Nil,
    classpath: Classpath = Classpath(Nil),
    ls: Ls = Ls.Find,
    cwd: AbsolutePath = PathIO.workingDirectory,
    sourceroot: List[AbsolutePath] = Nil,
    @ExtraName("remainingArgs")
    files: List[PathMatcher] = Nil,
    exclude: List[PathMatcher] = Nil,
    out: PrintStream = System.out,
    parser: MetaParser = MetaParser(),
    charset: Charset = StandardCharsets.UTF_8,
    stdout: Boolean = false,
    test: Boolean = false,
    metacpCacheDir: List[AbsolutePath] = Nil,
    metacpParallel: Boolean = false,
    settings: ScalafixConfig = ScalafixConfig(),
    format: OutputFormat = OutputFormat.Default,
    outFrom: List[String] = Nil,
    outTo: List[String] = Nil
) {

  def sourcerootPath: AbsolutePath = sourceroot.headOption.getOrElse(cwd)

  def withOut(out: PrintStream): Args = copy(
    out = out,
    settings = settings.withFreshReporters(out)
  )

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

  def getClassloader: ClassLoader =
    if (toolClasspath.isEmpty) ClassloadRule.defaultClassloader
    else {
      new URLClassLoader(
        toolClasspath.iterator.map(_.toURI.toURL).toArray,
        ClassloadRule.defaultClassloader
      )
    }

  def configuredRules: Configured[Rules] = {
    val rulesConf = Conf.Lst(rules.map(Conf.fromString))
    val decoder = ScalafixReflectV1.decoder(settings.reporter, getClassloader)
    config match {
      case Nil =>
        decoder.read(rulesConf)
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
            decoder
              .read(rulesConf)
              .andThen(_.withConfig(fileConf))
          }
        }
    }
  }

  def resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] =
    (outFrom, outTo) match {
      case (Nil, Nil) => Ok(identity[AbsolutePath])
      case (from :: Nil, to :: Nil) =>
        try {
          val outFromPattern = Pattern.compile(from)
          def replacePath(file: AbsolutePath): AbsolutePath = AbsolutePath(
            Paths.get(
              URI.create(
                "file:" +
                  outFromPattern.matcher(file.toURI.getPath).replaceAll(to))
            )
          )
          Ok(replacePath _)
        } catch {
          case e: PatternSyntaxException =>
            ConfError
              .message(s"Invalid regex '$outFrom'! ${e.getMessage}")
              .notOk
        }
      case (from :: Nil, _) =>
        ConfError
          .message(s"--out-from $from must be accompanied with --out-to")
          .notOk
      case (_, to :: Nil) =>
        ConfError
          .message(s"--out-to $to must be accompanied with --out-from")
          .notOk
    }

  def validate: Configured[ValidatedArgs] = {
    (
      configuredSymtab |@|
        configuredRules |@|
        resolvedPathReplace
    ).map {
      case ((symtab, rulez), pathReplace) =>
        ValidatedArgs(
          this.copy(
            settings = settings.withFormat(
              format
            )
          ),
          symtab,
          rulez,
          pathReplace
        )
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
