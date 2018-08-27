package scalafix.testkit

import java.nio.file.Files
import scala.meta.AbsolutePath
import scala.meta.Classpath
import scala.meta.internal.io.PathIO
import scala.meta.io.RelativePath

/**
  * Input arguments to run scalafix testkit rules.
  *
  * @param inputClasspath
  *        The class directory of the input sources. This directory should contain a
  *        META-INF/semanticd sub-directory with SemanticDB files.
  * @param inputSourceDirectories
  *        The source directory of the input sources. This directory should contain Scala code to
  *        be fixed by Scalafix.
  * @param outputSourceDirectories
  *        The source directories of the expected output sources. These directories should contain
  *        Scala source files with the expected output after running Scalafix. When multiple directories
  *        are provided, the first directory that contains a source files with a matching relative path
  *        in inputSourceroot is used.
  */
final class TestkitProperties(
    val inputClasspath: Classpath,
    val inputSourceDirectories: List[AbsolutePath],
    val outputSourceDirectories: List[AbsolutePath],
    val sourceroot: AbsolutePath,
    val scalaVersion: String,
    val scalacOptions: List[String]
) {
  def resolveInput(path: RelativePath): AbsolutePath =
    resolveFrom(inputSourceDirectories, path)
  def resolveOutput(path: RelativePath): AbsolutePath =
    resolveFrom(outputSourceDirectories, path)
  private def resolveFrom(
      directories: List[AbsolutePath],
      path: RelativePath): AbsolutePath = {
    outputSourceDirectories
      .collectFirst {
        case dir if Files.exists(dir.resolve(path).toNIO) =>
          dir.resolve(path)
      }
      .getOrElse {
        throw new NoSuchElementException(path.toString())
      }
  }
  override def toString: String = {
    val map = Map(
      "inputSourceDirectories" -> inputSourceDirectories,
      "outputSourceDirectories" -> outputSourceDirectories,
      "inputClasspath" -> inputClasspath.syntax,
      "sourceroot" -> sourceroot,
      "scalaVersion" -> scalaVersion,
      "scalacOptions" -> scalacOptions
    )
    pprint.PPrinter.BlackWhite.tokenize(map).mkString
  }
}

object TestkitProperties {

  /** Loads TestkitProperties from resource "scalafix-testkit.properties" of this classloader. */
  def loadFromResources(): TestkitProperties = {
    import scala.collection.JavaConverters._
    val props = new java.util.Properties()
    val path = "scalafix-testkit.properties"
    val in = this.getClass.getClassLoader.getResourceAsStream(path)
    if (in == null) {
      sys.error(s"Failed to load resource $path")
    } else {
      val sprops = props.asScala
      try props.load(in)
      finally in.close()
      val sourceroot = sprops.get("sourceroot") match {
        case Some(root) => AbsolutePath(root)
        case None => PathIO.workingDirectory
      }
      val scalacOptions = sprops.get("scalacOptions") match {
        case Some(options) => options.split("\\|").toList
        case None => Nil
      }
      new TestkitProperties(
        Classpath(sprops("inputClasspath")),
        Classpath(sprops("inputSourceDirectories")).entries,
        Classpath(sprops("outputSourceDirectories")).entries,
        sourceroot,
        sprops.getOrElseUpdate(
          "scalaVersion",
          scala.util.Properties.versionNumberString
        ),
        scalacOptions
      )
    }
  }

}
