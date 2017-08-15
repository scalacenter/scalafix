addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.19")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC10")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"
