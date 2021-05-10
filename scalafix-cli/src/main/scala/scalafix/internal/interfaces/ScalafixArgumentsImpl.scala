package scalafix.internal.interfaces

import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.util
import java.util.Optional

import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.control.NoStackTrace

import scala.meta.io.AbsolutePath
import scala.meta.io.Classpath

import coursierapi.Repository
import metaconfig.Conf
import metaconfig.Configured
import scalafix.Versions
import scalafix.cli.ExitStatus
import scalafix.interfaces._
import scalafix.internal.config.ScalaVersion
import scalafix.internal.v1.Args
import scalafix.internal.v1.MainOps
import scalafix.internal.v1.Rules
import scalafix.v1.RuleDecoder

final case class ScalafixArgumentsImpl(args: Args = Args.default)
    extends ScalafixArguments {

  override def run(): Array[ScalafixError] = {
    val exit = MainOps.run(Array(), args)
    ScalafixErrorImpl.fromScala(exit)
  }

  override def evaluate(): ScalafixEvaluation = {
    args.validate match {
      case Configured.Ok(validated) =>
        MainOps.runWithResult(validated)
      case Configured.NotOk(err) =>
        ScalafixEvaluationImpl(ExitStatus.CommandLineError, Some(err.msg))
    }
  }

  override def withRules(rules: util.List[String]): ScalafixArguments =
    copy(args = args.copy(rules = rules.asScala.toList))

  override def withToolClasspath(
      customURLs: util.List[URL]
  ): ScalafixArguments =
    withToolClasspath(
      new URLClassLoader(customURLs.asScala.toArray, getClass.getClassLoader)
    )

  override def withToolClasspath(
      customURLs: util.List[URL],
      customDependenciesCoordinates: util.List[String]
  ): ScalafixArguments =
    withToolClasspath(
      customURLs,
      customDependenciesCoordinates,
      Repository.defaults()
    )

  override def withToolClasspath(
      customURLs: util.List[URL],
      customDependenciesCoordinates: util.List[String],
      repositories: util.List[Repository]
  ): ScalafixArguments = {
    val customDependenciesJARs = ScalafixCoursier.toolClasspath(
      repositories,
      customDependenciesCoordinates,
      Versions.scalaVersion
    )
    val extraURLs = customURLs.asScala ++ customDependenciesJARs.asScala
    val classLoader = new URLClassLoader(
      extraURLs.toArray,
      getClass.getClassLoader
    )
    withToolClasspath(classLoader)
  }

  override def withToolClasspath(
      classLoader: URLClassLoader
  ): ScalafixArguments =
    copy(args = args.copy(toolClasspath = classLoader))

  override def withPaths(paths: util.List[Path]): ScalafixArguments = {
    copy(
      args = args.copy(
        files = paths.asScala.iterator.map(AbsolutePath(_)(args.cwd)).toList
      )
    )
  }

  override def withExcludedPaths(
      matchers: util.List[PathMatcher]
  ): ScalafixArguments =
    copy(args = args.copy(exclude = matchers.asScala.toList))

  override def withWorkingDirectory(path: Path): ScalafixArguments = {
    require(path.isAbsolute, s"working directory must be relative: $path")
    copy(args = args.copy(cwd = AbsolutePath(path)))
  }

  override def withConfig(path: Optional[Path]): ScalafixArguments = {
    val abspath = Option(path.orElse(null)).map(p => AbsolutePath(p)(args.cwd))
    copy(args = args.copy(config = abspath))
  }

  override def withMode(mode: ScalafixMainMode): ScalafixArguments =
    mode match {
      case ScalafixMainMode.CHECK =>
        copy(args = args.copy(check = true))
      case ScalafixMainMode.IN_PLACE =>
        copy(args = args.copy(stdout = false))
      case ScalafixMainMode.STDOUT =>
        copy(args = args.copy(stdout = true))
      case ScalafixMainMode.AUTO_SUPPRESS_LINTER_ERRORS =>
        copy(args = args.copy(autoSuppressLinterErrors = true))
      case ScalafixMainMode.IN_PLACE_TRIGGERED =>
        copy(args = args.copy(triggered = true))
    }

  override def withParsedArguments(
      args: util.List[String]
  ): ScalafixArguments = {
    if (args.isEmpty) this
    else {
      val decoder = Args.decoder(this.args)
      val newArgs = Conf
        .parseCliArgs[Args](args.asScala.toList)
        .andThen(c => c.as[Args](decoder)) match {
        case Configured.Ok(value) =>
          value
        case Configured.NotOk(error) =>
          throw new IllegalArgumentException(error.toString())
      }
      copy(args = newArgs)
    }
  }

  override def withPrintStream(out: PrintStream): ScalafixArguments =
    copy(args = args.copy(out = out))

  override def withClasspath(path: util.List[Path]): ScalafixArguments =
    copy(
      args = args.copy(
        classpath =
          Classpath(path.asScala.iterator.map(AbsolutePath(_)(args.cwd)).toList)
      )
    )

  override def withSourceroot(path: Path): ScalafixArguments = {
    require(path.isAbsolute, s"sourceroot must be relative: $path")
    copy(args = args.copy(sourceroot = Some(AbsolutePath(path)(args.cwd))))
  }

  override def withSemanticdbTargetroot(path: Path): ScalafixArguments = {
    copy(args =
      args.copy(semanticdbTargetroot = Some(AbsolutePath(path)(args.cwd)))
    )
  }

  override def withMainCallback(
      callback: ScalafixMainCallback
  ): ScalafixArguments =
    copy(args = args.copy(callback = callback))

  override def withCharset(charset: Charset): ScalafixArguments =
    copy(args = args.copy(charset = charset))

  override def availableRules(): util.List[ScalafixRule] = {
    Rules
      .all(args.toolClasspath)
      .map(rule => ScalafixRuleImpl(rule))
      .asJava
  }

  override def rulesThatWillRun(): util.List[ScalafixRule] = {
    val decoder = RuleDecoder.decoder(args.ruleDecoderSettings)
    val rules = decoder
      .read(args.rulesConf(() => args.fileConfig.getOrException))
      .getOrException
    rules.rules.map(rule => ScalafixRuleImpl(rule)).asJava
  }

  override def withScalacOptions(
      options: util.List[String]
  ): ScalafixArguments =
    copy(args = args.copy(scalacOptions = options.asScala.toList))

  override def withScalaVersion(version: String): ScalafixArguments = {
    ScalaVersion
      .from(version) match {
      case Success(value) => copy(args = args.copy(scalaVersion = value))
      case Failure(exception) =>
        throw new ScalafixMainArgsException(
          "Failed to parse the Scala version",
          exception
        )
    }
  }

  override def validate(): Optional[ScalafixException] = {
    args.validate match {
      case Configured.Ok(_) =>
        Optional.empty()
      case Configured.NotOk(error) =>
        Optional.of(new ScalafixMainArgsException(error.toString()))
    }
  }

  implicit class XtensionConfigured[T](c: Configured[T]) {
    def getOrException: T = c match {
      case Configured.Ok(value) => value
      case Configured.NotOk(error) =>
        throw new ScalafixMainArgsException(error.toString())
    }
  }
}

class ScalafixMainArgsException(msg: String, cause: Throwable)
    extends ScalafixException(msg, cause)
    with NoStackTrace {
  def this(msg: String) = this(msg, null)
}
