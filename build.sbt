lazy val v = _root_.scalafix.sbt.BuildInfo

inThisBuild(
  List(
    organization := "com.github.liancheng",
    homepage := Some(url("https://github.com/liancheng/scalafix-organize-imports")),
    licenses := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    version := "0.1.1-SNAPSHOT",
    developers := List(
      Developer(
        "liancheng",
        "Cheng Lian",
        "lian.cs.zju@gmail.com",
        url("https://github.com/liancheng")
      )
    ),
    scalaVersion := v.scala212,
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions ++= List(
      "-Yrangepos",
      "-P:semanticdb:synthetics:on"
    )
  )
)

skip in publish := true

lazy val rules = project
  .settings(
    moduleName := "organize-imports",
    conflictManager := ConflictManager.strict,
    dependencyOverrides += "com.lihaoyi" %% "sourcecode" % "0.2.1",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % v.scalafixVersion
  )

lazy val input = project.settings(skip in publish := true)

lazy val output = project.settings(skip in publish := true)

lazy val tests = project
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
  .settings(
    skip in publish := true,
    libraryDependencies +=
      "ch.epfl.scala" % "scalafix-testkit" % v.scalafixVersion % Test cross CrossVersion.full,
    dependencyOverrides ++= List(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.slf4j" % "slf4j-api" % "1.7.25"
    ),
    (compile in Compile) := ((compile in Compile) dependsOn (compile in (input, Compile))).value,
    scalafixTestkitOutputSourceDirectories := (sourceDirectories in (output, Compile)).value,
    scalafixTestkitInputSourceDirectories := (sourceDirectories in (input, Compile)).value,
    scalafixTestkitInputClasspath := (fullClasspath in (input, Compile)).value
  )
