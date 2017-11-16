addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC13")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"
