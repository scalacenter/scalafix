addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.2.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
addSbtPlugin(
  "io.get-coursier" % "sbt-coursier" % coursier.util.Properties.version)
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.2.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.2")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.3.2")
// exclude is a workaround for https://github.com/sbt/sbt-assembly/issues/236#issuecomment-294452474
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
addSbtPlugin("com.47deg" % "sbt-microsites" % "0.7.22")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("com.geirsson" % "sbt-docusaurus" % "0.1.0")
