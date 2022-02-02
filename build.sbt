lazy val v = _root_.scalafix.sbt.BuildInfo
lazy val rulesCrossVersions = Seq(v.scala213, v.scala212, v.scala211)
lazy val scala3Version = "3.1.1"

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
    scalacOptions ++= List(
      "-deprecation"
    ),
    semanticdbEnabled := true,
    // semanticdbTargetRoot makes it hard to have several input modules
    semanticdbIncludeInJar := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0",
    // Super shell output often messes up Scalafix test output.
    useSuperShell := false
  )
)

lazy val `scalafix-organize-imports` = project
  .in(file("."))
  .aggregate(
    rules.projectRefs ++
      input.projectRefs ++
      output.projectRefs ++
      tests.projectRefs: _*
  )
  .settings(
    publish / skip := true
  )

lazy val rules = projectMatrix
  .settings(
    moduleName := "organize-imports",
    conflictManager := ConflictManager.strict,
    dependencyOverrides ++= List(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6",
      "com.lihaoyi" %% "sourcecode" % "0.2.1"
    ),
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % v.scalafixVersion,
    scalacOptions ++= List("-Ywarn-unused"),
    scalafixOnCompile := true
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions)

lazy val shared = projectMatrix
  .settings(
    publish / skip := true,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

lazy val input = projectMatrix
  .dependsOn(shared)
  .settings(
    publish / skip := true,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

lazy val output = projectMatrix
  .dependsOn(shared)
  .settings(
    publish / skip := true,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

lazy val inputUnusedImports = projectMatrix
  .dependsOn(shared)
  .settings(
    publish / skip := true,
    coverageEnabled := false,
    scalacOptions += {
      if (scalaVersion.value.startsWith("2.11."))
        "-Ywarn-unused-import"
      else
        "-Ywarn-unused"
    }
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

lazy val testsAggregate = Project("tests", file("target/testsAggregate"))
  .aggregate(tests.projectRefs: _*)

lazy val tests = projectMatrix
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
  .settings(
    publish / skip := true,
    coverageEnabled := false,
    libraryDependencies +=
      "ch.epfl.scala" % "scalafix-testkit" % v.scalafixVersion % Test cross CrossVersion.full,
    scalafixTestkitOutputSourceDirectories :=
      TargetAxis.resolve(output, Compile / unmanagedSourceDirectories).value,
    scalafixTestkitInputSourceDirectories := {
      val inputSrc =
        TargetAxis.resolve(input, Compile / unmanagedSourceDirectories).value
      val inputUnusedImportsSrc =
        TargetAxis.resolve(inputUnusedImports, Compile / unmanagedSourceDirectories).value
      inputSrc ++ inputUnusedImportsSrc
    },
    scalafixTestkitInputClasspath := {
      val inputClasspath =
        TargetAxis.resolve(input, Compile / fullClasspath).value
      val inputUnusedImportsClasspath =
        TargetAxis.resolve(inputUnusedImports, Compile / fullClasspath).value
      inputClasspath ++ inputUnusedImportsClasspath
    },
    scalafixTestkitInputScalacOptions :=
      TargetAxis.resolve(inputUnusedImports, Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      TargetAxis.resolve(inputUnusedImports, Compile / scalaVersion).value
  )
  .defaultAxes(
    rulesCrossVersions.map(VirtualAxis.scalaABIVersion) :+ VirtualAxis.jvm: _*
  )
  .customRow(
    scalaVersions = Seq(v.scala212),
    axisValues = Seq(TargetAxis(scala3Version), VirtualAxis.jvm),
    settings = Seq()
  )
  .customRow(
    scalaVersions = Seq(v.scala213),
    axisValues = Seq(TargetAxis(v.scala213), VirtualAxis.jvm),
    settings = Seq()
  )
  .customRow(
    scalaVersions = Seq(v.scala212),
    axisValues = Seq(TargetAxis(v.scala212), VirtualAxis.jvm),
    settings = Seq()
  )
  .customRow(
    scalaVersions = Seq(v.scala211),
    axisValues = Seq(TargetAxis(v.scala211), VirtualAxis.jvm),
    settings = Seq()
  )
