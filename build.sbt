import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

inThisBuild(List(
  organization := "com.github.masseguillaume",
  homepage := Some(url("https://github.com/MasseGuillaume/scalameta-structure")),
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  developers := List(
    Developer(
      "MasseGuillaume",
      "Guillaume Massé",
      "masgui@gmail.com",
      url("https://github.com/MasseGuillaume")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/MasseGuillaume/scalameta-structure"),
      s"scm:git:git@github.com:MasseGuillaume/scalameta-structure.git"
    )
  )
))

lazy val `scalameta-structure` =
  project
    .in(file("."))
    .settings(
      publishLocal := {},
      publish := {}
    )
    .aggregate(structureJS, structureJVM)//, structureNative)

lazy val structure = crossProject(JSPlatform, JVMPlatform)//, NativePlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .settings(
    
    moduleName := "scalameta-structure",
    version := "0.1.0",

    // scalaVersion := "2.11.12",
    scalaVersion := "2.12.6",

    libraryDependencies ++= List(
      "org.typelevel" %%% "paiges-cats" % "0.2.1",
      "org.scalameta" %%% "scalameta"   % "2.1.7",
      // "org.scalameta" %%% "scalameta"   % "4.0.0-M4",
      "com.lihaoyi"   %%% "utest"       % "0.6.3"    % Test
    ),
    testFrameworks += new TestFramework("utest.runner.Framework"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-unchecked"
    )
  )


lazy val structureJS     = structure.js
lazy val structureJVM    = structure.jvm
// lazy val structureNative = structure.native
