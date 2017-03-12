libraryDependencies += "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % sys.props("plugin.version"))
