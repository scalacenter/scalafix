resolvers += Resolver.sonatypeRepo("releases")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.29")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")
