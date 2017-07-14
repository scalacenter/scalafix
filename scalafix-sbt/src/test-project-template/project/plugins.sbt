libraryDependencies += "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"
resolvers += Resolver.bintrayIvyRepo("scalameta", "sbt-plugins")

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % sys.props("plugin.version"))
