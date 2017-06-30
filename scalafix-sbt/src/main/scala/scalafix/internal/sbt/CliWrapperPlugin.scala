package scalafix.internal.sbt

import sbt.AutoPlugin
import sbt.Def
import sbt.File
import sbt.Keys.Classpath
import sbt.Keys.publish
import sbt.Keys.publishArtifact
import sbt.Keys.publishLocal
import sbt.PluginTrigger
import sbt.Plugins
import sbt.Project
import sbt.TaskKey
import sbt.plugins.JvmPlugin
import sbt.taskKey

// generic plugin for wrapping any command-line interface as an sbt plugin
object CliWrapperPlugin extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins = JvmPlugin
  def createSyntheticProject(id: String, base: File): Project =
    Project(id, base).settings(publish := {},
                               publishLocal := {},
                               publishArtifact := false)
  class HasMain(reflectiveMain: Main) {
    def main(args: Array[String]): Unit = reflectiveMain.main(args)
  }
  type Main = {
    def main(args: Array[String]): Unit
  }
  object autoImport {
    val cliWrapperClasspath: TaskKey[Classpath] =
      taskKey[Classpath]("classpath to run code generation in")
    val cliWrapperMainClass: TaskKey[String] =
      taskKey[String]("Fully qualified name of main class")
    val cliWrapperMain: TaskKey[HasMain] =
      taskKey[HasMain]("Classloaded instance of main")
  }
  import autoImport._
  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    cliWrapperMain := {
      val cp = cliWrapperClasspath.value.map(_.data.toURI.toURL)
      val cl = new java.net.URLClassLoader(cp.toArray, null)
      val cls = cl.loadClass(cliWrapperMainClass.value)
      val constuctor = cls.getDeclaredConstructor()
      constuctor.setAccessible(true)
      val main = constuctor.newInstance().asInstanceOf[Main]
      new HasMain(main)
    }
  )
}
