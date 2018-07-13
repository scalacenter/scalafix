package scalafix.testkit

import scala.meta.AbsolutePath
import scala.meta.Classpath
import scala.meta.internal.io.PathIO

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
    val sourceroot: AbsolutePath
) {
  def inputSourceDirectory: AbsolutePath =
    inputSourceDirectories.head
  def outputSourceDirectory: AbsolutePath =
    outputSourceDirectories.head
  override def toString: String = {
    val map = Map(
      "inputSourceDirectories" -> inputSourceDirectories,
      "outputSourceDirectories" -> outputSourceDirectories,
      "inputClasspath" -> inputClasspath.syntax,
      "sourceroot" -> sourceroot
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
      new TestkitProperties(
        Classpath(sprops("inputClasspath")),
        Classpath(sprops("inputSourceDirectories")).entries,
        Classpath(sprops("outputSourceDirectories")).entries,
        sourceroot
      )
    }
  }

}
