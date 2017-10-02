addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")
addSbtPlugin("com.lihaoyi" % "scalatex-sbt-plugin" % "0.3.9")
addSbtPlugin(
  "io.get-coursier" % "sbt-coursier" % coursier.util.Properties.version)
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.2.0")
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.1.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.2.0")
// exclude is a workaround for https://github.com/sbt/sbt-assembly/issues/236#issuecomment-294452474
addSbtPlugin(
  "com.eed3si9n" % "sbt-assembly" % "0.14.5" exclude ("org.apache.maven", "maven-plugin-api"))
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.18")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.7.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.15")
addSbtPlugin("com.47deg" % "sbt-microsites" % "0.7.0")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.1")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
