resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.30")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.9")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.8.0")
