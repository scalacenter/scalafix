resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.33")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.5")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.2")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.0")
