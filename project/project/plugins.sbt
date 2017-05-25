addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-M15")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"
