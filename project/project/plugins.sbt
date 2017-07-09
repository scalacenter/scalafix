addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.17")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"
