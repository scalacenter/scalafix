lazy val v = _root_.scalafix.sbt.BuildInfo

inThisBuild(
  List(
    organization := "com.github.liancheng",
    homepage := Some(url("https://github.com/liancheng/scalafix-organize-imports")),
    licenses := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    developers := List(
      Developer(
        "liancheng",
        "Cheng Lian",
        "lian.cs.zju@gmail.com",
        url("https://github.com/liancheng")
      )
    ),
    scalaVersion := v.scala212,
    crossScalaVersions := List(v.scala211, v.scala212, v.scala213),
    scalacOptions ++= List(
      "-deprecation",
      "-Yrangepos",
      "-P:semanticdb:synthetics:on"
    ),
    conflictManager := ConflictManager.strict,
    dependencyOverrides ++= List(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.slf4j" % "slf4j-api" % "1.7.25",
      "com.lihaoyi" %% "sourcecode" % "0.2.1",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6"
    ),
    addCompilerPlugin(scalafixSemanticdb),
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.4.4",
    // Super shell output often messes up Scalafix test output.
    useSuperShell := false
  )
)

skip in publish := true

lazy val rules = project
  .settings(
    moduleName := "organize-imports",
    dependencyOverrides += "com.lihaoyi" %% "sourcecode" % "0.2.1",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % v.scalafixVersion,
    scalacOptions ++= List("-Ywarn-unused"),
    scalafixOnCompile := true
  )

lazy val shared = project.settings(skip in publish := true)

lazy val input = project
  .dependsOn(shared)
  .settings(skip in publish := true)

lazy val output = project
  .dependsOn(shared)
  .settings(skip in publish := true)

lazy val inputUnusedImports = project
  .dependsOn(shared)
  .settings(
    skip in publish := true,
    scalacOptions += {
      if (scalaVersion.value.startsWith("2.11."))
        "-Ywarn-unused-import"
      else
        "-Ywarn-unused"
    }
  )

lazy val tests = project
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
  .settings(
    skip in publish := true,
    scalacOptions ++= List("-Ywarn-unused"),
    libraryDependencies +=
      "ch.epfl.scala" % "scalafix-testkit" % v.scalafixVersion % Test cross CrossVersion.full,
    (compile in Compile) := (compile in Compile)
      .dependsOn(
        compile in (input, Compile),
        compile in (inputUnusedImports, Compile)
      )
      .value,
    scalafixTestkitOutputSourceDirectories := sourceDirectories.in(output, Compile).value,
    scalafixTestkitInputSourceDirectories := (
      sourceDirectories.in(input, Compile).value ++
        sourceDirectories.in(inputUnusedImports, Compile).value
    ),
    scalafixTestkitInputClasspath := (
      fullClasspath.in(input, Compile).value ++
        fullClasspath.in(inputUnusedImports, Compile).value
    ).distinct
  )
