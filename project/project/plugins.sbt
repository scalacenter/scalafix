addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.18")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0-RC6")
unmanagedSources.in(Compile) += baseDirectory.value / ".." / "Dependencies.scala"

resolvers += Resolver.sonatypeRepo("staging")
