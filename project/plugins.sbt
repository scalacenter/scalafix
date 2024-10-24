addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.8.0")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.12.0")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")
addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.1")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.6.1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.2.1")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.13.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.10.0")

// https://github.com/scala/bug/issues/12632
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % "always"
