addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.2.6")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")
// exclude is a workaround for https://github.com/sbt/sbt-assembly/issues/236#issuecomment-294452474
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.4.2")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "1.2.10")
