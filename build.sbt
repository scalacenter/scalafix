lazy val v = _root_.scalafix.sbt.BuildInfo

ThisBuild / organization := "com.github.liancheng"
ThisBuild / scalaVersion := v.scala212
ThisBuild / (skip in publish) := true
ThisBuild / scalacOptions ++= Seq("-Yrangepos", "-P:semanticdb:synthetics:on")
ThisBuild / conflictManager := ConflictManager.strict
ThisBuild / libraryDependencies += compilerPlugin(scalafixSemanticdb)
ThisBuild / dependencyOverrides += "com.lihaoyi" %% "sourcecode" % "0.2.1"

lazy val rules = project
  .settings(
    moduleName := "scalafix",
    libraryDependencies +=
      "ch.epfl.scala" %% "scalafix-core" % v.scalafixVersion
  )

lazy val input = project.settings(skip in publish := true)

lazy val output = project.settings(skip in publish := true)

lazy val tests = project
  .settings(
    skip in publish := true,
    libraryDependencies +=
      "ch.epfl.scala" % "scalafix-testkit" % v.scalafixVersion % Test cross CrossVersion.full,
    dependencyOverrides ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.slf4j" % "slf4j-api" % "1.7.25"
    )
  )
  .settings(
    (compile in Compile) :=
      ((compile in Compile) dependsOn (compile in (input, Compile))).value,
    scalafixTestkitOutputSourceDirectories :=
      (sourceDirectories in (output, Compile)).value,
    scalafixTestkitInputSourceDirectories :=
      (sourceDirectories in (input, Compile)).value,
    scalafixTestkitInputClasspath :=
      (fullClasspath in (input, Compile)).value
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
