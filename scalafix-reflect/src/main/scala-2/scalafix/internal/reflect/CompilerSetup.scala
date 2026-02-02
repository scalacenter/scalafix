package scalafix.internal.reflect

import scala.reflect.io.AbstractFile
import scala.tools.nsc.Settings

object CompilerSetup {

  def newSettings(classpath: String, output: AbstractFile): Settings = {
    val settings = new Settings()
    settings.deprecation.value = true // enable detailed deprecation warnings
    settings.unchecked.value = true // enable detailed unchecked warnings
    settings.classpath.value = classpath
    settings.outputDirs.setSingleOutput(output)
    settings
  }
}
