addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.23")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M4")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"
