package scalafix.internal.reflect

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import coursierapi.Dependency
import coursierapi.Fetch
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.Input
import metaconfig.Position
import scalafix.Versions

/**
 * Compiles code with Scala 2.13 in an isolated classloader environment where
 * the standard library matching the compiler one is fetched via coursier and
 * replaces the runtime one.
 *
 * See https://github.com/scala/scala3/issues/25145 for the rationale.
 */
object IsolatedScala213Compiler {

  def compile(
      input: Input,
      toolClasspath: URLClassLoader,
      targetDirectory: Option[File]
  ): Configured[URLClassLoader] = {
    try {
      val outputDir = targetDirectory match {
        case Some(dir) =>
          dir.mkdirs()
          dir.toPath
        case None =>
          val timestamp = System.currentTimeMillis()
          Files.createTempDirectory(s"scalafix-reflect-bridge-$timestamp")
      }

      val classpathJars = classpathWithStdLibOverride(toolClasspath)

      val classLoader = new URLClassLoader(
        classpathJars.map(_.toURI.toURL).toArray,
        null
      )

      val classpath = classpathJars
        .map(_.getAbsolutePath)
        .mkString(File.pathSeparator)

      val result = invokeBridge(
        classLoader,
        input,
        classpath,
        outputDir.toAbsolutePath.toString
      )

      result match {
        case Left(errors) =>
          ConfError
            .fromResults(errors.map(_.notOk))
            .map(_.notOk)
            .getOrElse(
              ConfError.message("Compilation failed").notOk
            )
        case Right(outputDir) =>
          val classLoader = new URLClassLoader(
            Array(new File(outputDir).toURI.toURL),
            toolClasspath
          )
          Configured.ok(classLoader)
      }
    } catch {
      case NonFatal(ex) =>
        ConfError
          .message(
            s"Failed to compile in isolated environment: ${ex.getMessage}"
          )
          .notOk
    }
  }

  private def classpathWithStdLibOverride(
      toolClasspath: URLClassLoader
  ): Seq[File] = {
    def isScalaLibrary(f: File): Boolean =
      f.getName.startsWith("scala-library")

    val classpathWithoutStdLib = (
      toolClasspath.getURLs.map(url => new File(url.toURI)) ++
        RuleCompilerClasspath.defaultClasspathPaths.map(_.toFile)
    ).filterNot(isScalaLibrary)

    classpathWithoutStdLib ++ fetchScala213LibraryJars()
  }

  private def fetchScala213LibraryJars(): Seq[File] = {
    val scalaLibrary =
      Dependency.of("org.scala-lang", "scala-library", Versions.scala213)
    Fetch.create().addDependencies(scalaLibrary).fetch().asScala.toSeq
  }

  private def invokeBridge(
      isolatedReflectLoader: URLClassLoader,
      input: Input,
      classpath: String,
      outputDir: String
  ): Either[Seq[ConfError], String] = {
    val bridgeClass = isolatedReflectLoader.loadClass(
      "scalafix.internal.reflect.bridge.Scala2CompilerBridge"
    )
    val bridge = bridgeClass.getConstructor().newInstance()

    val sourceCode = new String(input.chars)
    val filename = input match {
      case Input.File(path, _) => path.toString
      case Input.VirtualFile(path, _) => path
      case _ => "(input)"
    }

    val compileMethod = bridgeClass.getMethod(
      "compile",
      classOf[String],
      classOf[String],
      classOf[String],
      classOf[String]
    )

    val result =
      compileMethod.invoke(bridge, sourceCode, filename, classpath, outputDir)

    val resultClass = result.getClass
    val successField = resultClass.getField("success")
    val messagesField = resultClass.getField("messages")

    val success = successField.get(result).asInstanceOf[Boolean]
    val messages = messagesField.get(result).asInstanceOf[java.util.List[_]]

    if (success) {
      Right(outputDir)
    } else {
      val confErrors = messages.asScala.collect {
        case msg if {
              val msgClass = msg.getClass
              val severityField = msgClass.getField("severity")
              val severity = severityField.get(msg).asInstanceOf[String]
              severity == "ERROR"
            } =>
          val msgClass = msg.getClass
          val startField = msgClass.getField("start")
          val endField = msgClass.getField("end")
          val textField = msgClass.getField("text")

          val start = startField.get(msg).asInstanceOf[Int]
          val end = endField.get(msg).asInstanceOf[Int]
          val text = textField.get(msg).asInstanceOf[String]

          ConfError
            .message(text)
            .atPos(
              if (start > 0 && end > 0) Position.Range(input, start, end)
              else Position.None
            )
      }

      Left(confErrors.toSeq)
    }
  }
}
