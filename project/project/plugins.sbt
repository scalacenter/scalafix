addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.23")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.2")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"
