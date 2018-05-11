package scalafix.v1

import java.io.PrintStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import metaconfig._
import metaconfig.generic.Surface
import org.langmeta.internal.io.FileIO
import scala.meta.Source
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scala.meta.parsers.Parsed
import scalafix.internal.v1.Rules
import scala.meta.Input
import scalafix.internal.cli.ClasspathOps
import scalafix.internal.cli.WriteMode

case class Args(
    rules: Rules = Rules(),
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

object Args {
  val baseMatcher = FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt}")
  val scalaMatcher = FileSystems.getDefault.getPathMatcher("glob:*.sbt")

  implicit val surface: Surface[Args] = generic.deriveSurface
  implicit val decoder: ConfDecoder[Args] = generic.deriveDecoder(Args())

  implicit val rulesDecoder: ConfDecoder[Rules] = // TODO
    ConfDecoder.instanceF[Rules](_ => Configured.ok(Rules(Nil)))
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
