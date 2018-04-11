package scalafix.internal.sbt

import java.net.URL
import java.net.URLClassLoader
import java.util
import sbt.AutoPlugin
import sbt.Def
import sbt.File
import sbt.PluginTrigger
import sbt.Plugins
import sbt.TaskKey
import sbt.plugins.JvmPlugin
import sbt.taskKey

// generic plugin for wrapping any command-line interface as an sbt plugin
object CliWrapperPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  class HasMain(reflectiveMain: Main) {
    import scala.language.reflectiveCalls
    def main(args: Array[String]): Unit = reflectiveMain.main(args)
  }
  type Main = {
    def main(args: Array[String]): Unit
  }
  object autoImport {
    val cliWrapperClasspath: TaskKey[Seq[File]] =
      taskKey[Seq[File]]("classpath to run code generation in")
    val cliWrapperMainClass: TaskKey[String] =
      taskKey[String]("Fully qualified name of main class")
    val cliWrapperMain: TaskKey[HasMain] =
      taskKey[HasMain]("Classloaded instance of main")
  }
  import autoImport._
  private val cachedScalafixMain =
    util.Collections.synchronizedMap(
      new util.HashMap[(Seq[URL], String), HasMain]())
  private val computeClassloader =
    new util.function.Function[(Seq[URL], String), HasMain] {
      override def apply(t: (Seq[URL], String)): HasMain = {
        val (classpath, cliWrapperMainClass) = t
        val classloader = new URLClassLoader(classpath.toArray, null)
        val clazz = classloader.loadClass(cliWrapperMainClass)
        val constructor = clazz.getDeclaredConstructor()
        constructor.setAccessible(true)
        val main = constructor.newInstance().asInstanceOf[Main]
        new HasMain(main)
      }
    }

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperMain := {
      val cp = cliWrapperClasspath.value.map(_.toURI.toURL)
      val main = cachedScalafixMain.computeIfAbsent(
        cp -> cliWrapperMainClass.value,
        computeClassloader
      )
      main
    }
  )
}
