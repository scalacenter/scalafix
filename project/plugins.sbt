addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.2")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.10.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.4.0")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.9.2")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.4.4")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.10")

// 2.1.0 (transitive dep of sbt-ghpages 0.10.0) was built against sbt 2.0.0-RC2, whose `:=` macro
// stores the setting source in the position path, tripping sbt 2's unused-keys lint.
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.2.0")
