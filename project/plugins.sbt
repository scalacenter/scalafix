addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "2.1.3")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.7")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.1")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.1")

// https://github.com/scala/bug/issues/12632
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"
