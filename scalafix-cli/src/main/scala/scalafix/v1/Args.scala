package scalafix.v1

import java.io.File
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
import metaconfig.annotation.Description
import metaconfig.annotation.ExtraName
import metaconfig.generic.Surface
import metaconfig.internal.ConfGet
import metaconfig.typesafeconfig.typesafeConfigMetaconfigParser
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath
import scalafix.cli.CliRunner
import scalafix.internal.config.OutputFormat
import scalafix.internal.config.ScalafixConfig
import scalafix.internal.reflect.ClasspathOps
import scalafix.internal.util.ClassloadRule
import scalafix.internal.util.SymbolTable
import scalafix.internal.v1.Rules
import scalafix.reflect.ScalafixReflectV1

case class Args(
    cwd: AbsolutePath,
    out: PrintStream,
    @ExtraName("r")
    rules: List[String] = Nil,
    config: Option[AbsolutePath] = None,
    toolClasspath: List[AbsolutePath] = Nil,
    classpath: Classpath = Classpath(Nil),
    ls: Ls = Ls.Find,
    sourceroot: Option[AbsolutePath] = None,
    @ExtraName("remainingArgs")
    files: List[AbsolutePath] = Nil,
    exclude: List[PathMatcher] = Nil,
    parser: MetaParser = MetaParser(),
    charset: Charset = StandardCharsets.UTF_8,
    stdout: Boolean = false,
    test: Boolean = false,
    metacpCacheDir: List[AbsolutePath] = Nil,
    metacpParallel: Boolean = false,
    autoClasspath: Boolean = false,
    settings: Conf = Conf.Obj.empty,
    format: OutputFormat = OutputFormat.Default,
    outFrom: Option[String] = None,
    outTo: Option[String] = None,
    @Description(
      "Write to files. In case of linter error adds a comment to suppress the error.")
    autoSuppressLinterErrors: Boolean = false,
    @Description(
      "Automatically infer --classpath starting from these directories. " +
        "Ignored if --classpath is provided.")
    autoClasspathRoots: List[AbsolutePath] = Nil,
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

  def getClassloader: ClassLoader =
    if (toolClasspath.isEmpty) ClassloadRule.defaultClassloader
    else {
      new URLClassLoader(
        toolClasspath.iterator.map(_.toURI.toURL).toArray,
        ClassloadRule.defaultClassloader
      )
    }

  def baseConfig: Configured[(Conf, ScalafixConfig)] = {
    val toRead: Option[AbsolutePath] = config.orElse {
      val defaultPath = cwd.resolve(".scalafix.conf")
      if (defaultPath.isFile) Some(defaultPath)
      else None
    }
    val base = toRead match {
      case Some(file) =>
        if (file.isFile) {
          val input = metaconfig.Input.File(file.toNIO)
          Conf.parseInput(input)
        } else {
          ConfError.fileDoesNotExist(file.toNIO).notOk
        }
      case _ =>
        Configured.ok(Conf.Obj.empty)
    }
    base.andThen { b =>
      val applied = Conf.applyPatch(b, settings)
      applied.as[ScalafixConfig].map { scalafixConfig =>
        applied -> scalafixConfig.withOut(out)
      }
    }
  }

  def configuredRules(
      base: Conf,
      scalafixConfig: ScalafixConfig
  ): Configured[Rules] = {
    val rulesConf =
      if (rules.isEmpty) {
        ConfGet.getKey(base, "rules" :: "rule" :: Nil) match {
          case Some(c) => c
          case _ => Conf.Lst(Nil)
        }
      } else {
        Conf.Lst(rules.map(Conf.fromString))
      }
    val decoder =
      ScalafixReflectV1.decoder(scalafixConfig.reporter, getClassloader)
    decoder.read(rulesConf).andThen { rules =>
      if (rules.isEmpty) ConfError.message("No rules provided").notOk
      else rules.withConfig(base)
    }
  }

  def resolvedPathReplace: Configured[AbsolutePath => AbsolutePath] =
    (outFrom, outTo) match {
      case (None, None) => Ok(identity[AbsolutePath])
      case (Some(from), Some(to)) =>
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
      case (Some(from), _) =>
        ConfError
          .message(s"--out-from $from must be accompanied with --out-to")
          .notOk
      case (_, Some(to)) =>
        ConfError
          .message(s"--out-to $to must be accompanied with --out-from")
          .notOk
    }

  def configuredSourceroot: Configured[AbsolutePath] = {
    val path = sourceroot.getOrElse(cwd)
    if (path.isDirectory) Configured.ok(path)
    else Configured.error(s"--sourceroot $path is not a directory")
  }

  def validatedClasspath: Classpath = {
    if (autoClasspath && classpath.shallow.isEmpty) {
      val roots =
        if (autoClasspathRoots.isEmpty) cwd :: Nil
        else autoClasspathRoots
      CliRunner.autoClasspath(roots)
    } else classpath
  }

  def validate: Configured[ValidatedArgs] = {
    baseConfig.andThen {
      case (base, scalafixConfig) =>
        (
          configuredSourceroot |@|
            configuredSymtab |@|
            configuredRules(base, scalafixConfig) |@|
            resolvedPathReplace
        ).map {
          case (((root, symtab), rulez), pathReplace) =>
            ValidatedArgs(
              this,
              symtab,
              rulez,
              scalafixConfig.withFormat(
                format
              ),
              validatedClasspath,
              root,
              pathReplace
            )
        }
    }
  }
}

object Args {
  val baseMatcher: PathMatcher =
    FileSystems.getDefault.getPathMatcher("glob:**.{scala,sbt}")
  val default = new Args(PathIO.workingDirectory, System.out)

  implicit val surface: Surface[Args] = generic.deriveSurface
  def decoder(cwd: AbsolutePath, out: PrintStream): ConfDecoder[Args] = {
    implicit val classpathDecoder: ConfDecoder[Classpath] =
      ConfDecoder.stringConfDecoder.map { cp =>
        Classpath(
          cp.split(File.pathSeparator)
            .iterator
            .map(path => AbsolutePath(path)(cwd))
            .toList
        )
      }
    implicit val absolutePathDecoder: ConfDecoder[AbsolutePath] =
      ConfDecoder.stringConfDecoder.map(AbsolutePath(_)(cwd))
    generic.deriveDecoder(Args(cwd, out))
  }

  implicit val confDecoder: ConfDecoder[Conf] = // TODO: upstream
    ConfDecoder.instanceF[Conf](c => Configured.ok(c))
  implicit val charsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.map(name => Charset.forName(name))
  implicit val printStreamDecoder: ConfDecoder[PrintStream] =
    ConfDecoder.stringConfDecoder.map(_ => System.out)
  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob =>
      FileSystems.getDefault.getPathMatcher("glob:" + glob))
}

case class ScalafixFileConfig(rules: Conf, other: Conf)
object ScalafixFileConfig {
  val empty = ScalafixFileConfig(
    Conf.Obj.empty,
    Conf.Obj.empty
  )
}
