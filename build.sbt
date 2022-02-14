import Dependencies._
import sbt.Keys.scalacOptions

inThisBuild(
  List(
    onLoadMessage := s"Welcome to scalafix ${version.value}",
    scalaVersion := scala213,
    crossScalaVersions := List(scala213, scala212, scala211),
    fork := true,
    scalacOptions ++= List("-P:semanticdb:synthetics:on"),
    semanticdbEnabled := true,
    semanticdbVersion := scalametaV,
    scalafixScalaBinaryVersion := scalaBinaryVersion.value,
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
  )
)

Global / cancelable := true
noPublishAndNoMima

// force javac to fork by setting javaHome to get error messages during compilation,
// see https://github.com/sbt/zinc/issues/520
def inferJavaHome() = {
  val home = file(sys.props("java.home"))
  val actualHome =
    if (System.getProperty("java.version").startsWith("1.8")) home.getParentFile
    else home
  Some(actualHome)
}

lazy val interfaces = project
  .in(file("scalafix-interfaces"))
  .settings(
    Compile / resourceGenerators += Def.task {
      val props = new java.util.Properties()
      props.put("scalafixVersion", version.value)
      props.put("scalafixStableVersion", stableVersion.value)
      props.put("scalametaVersion", scalametaV)
      props.put("scala213", scala213)
      props.put("scala212", scala212)
      props.put("scala211", scala211)
      val out =
        (Compile / managedResourceDirectories).value.head /
          "scalafix-interfaces.properties"
      IO.write(props, "Scalafix version constants", out)
      List(out)
    },
    (Compile / javacOptions) ++= List(
      "-Xlint:all",
      "-Werror"
    ),
    (Compile / doc / javacOptions) := List("-Xdoclint:none"),
    (Compile / javaHome) := inferJavaHome(),
    (Compile / doc / javaHome) := inferJavaHome(),
    libraryDependencies += coursierInterfaces,
    moduleName := "scalafix-interfaces",
    crossPaths := false,
    autoScalaLibrary := false
  )
  .disablePlugins(ScalafixPlugin)

lazy val core = project
  .in(file("scalafix-core"))
  .settings(
    moduleName := "scalafix-core",
    buildInfoSettingsForCore,
    libraryDependencies ++= List(
      scalameta,
      googleDiff,
      collectionCompat
    ),
    libraryDependencies ++= {
      if (isScala211.value) Seq(metaconfigFor211)
      else
        Seq(
          metaconfig,
          // metaconfig 0.10.0 shaded pprint but rules built with an old
          // scalafix-core must have the original package in the classpath to link
          // https://github.com/scalameta/metaconfig/pull/154/files#r794005161
          pprint
        )
    }
  )
  .enablePlugins(BuildInfoPlugin)

lazy val rules = project
  .in(file("scalafix-rules"))
  .settings(
    moduleName := "scalafix-rules",
    description := "Built-in Scalafix rules",
    buildInfoSettingsForRules,
    libraryDependencies ++= List(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      semanticdbScalacCore,
      collectionCompat
    )
  )
  .dependsOn(core)
  .enablePlugins(BuildInfoPlugin)

lazy val reflect = project
  .in(file("scalafix-reflect"))
  .settings(
    moduleName := "scalafix-reflect",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
  .dependsOn(core)

lazy val cli = project
  .in(file("scalafix-cli"))
  .settings(
    moduleName := "scalafix-cli",
    isFullCrossVersion,
    assembly / mainClass := Some("scalafix.v1.Main"),
    assembly / assemblyJarName := "scalafix.jar",
    libraryDependencies ++= Seq(
      java8Compat,
      nailgunServer,
      jgit,
      commonText
    )
  )
  .dependsOn(reflect, interfaces, rules)

lazy val testsShared = project
  .in(file("scalafix-tests/shared"))
  .settings(
    noPublishAndNoMima,
    coverageEnabled := false
  )
  .disablePlugins(ScalafixPlugin)

lazy val testsInput = project
  .in(file("scalafix-tests/input"))
  .settings(
    noPublishAndNoMima,
    crossScalaVersions := List(scala3, scala213, scala212, scala211),
    scalacOptions --= (if (isScala3.value)
                         Seq("-P:semanticdb:synthetics:on")
                       else Nil),
    scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
    scalacOptions ++= warnAdaptedArgs.value, // For NoAutoTupling
    scalacOptions ++= warnUnusedImports.value, // For RemoveUnused
    scalacOptions ++= warnUnused.value, // For RemoveUnusedTerms
    logLevel := Level.Error, // avoid flood of compiler warnings
    libraryDependencies ++= testsDependencies.value,
    coverageEnabled := false
  )
  .disablePlugins(ScalafixPlugin)

lazy val testsOutput = project
  .in(file("scalafix-tests/output"))
  .settings(
    noPublishAndNoMima,
    scalacOptions --= (if (scalaVersion.value.startsWith("3"))
                         Seq("-P:semanticdb:synthetics:on")
                       else Nil),
    scalacOptions --= warnUnusedImports.value,
    libraryDependencies ++= testsDependencies.value,
    coverageEnabled := false
  )
  .disablePlugins(ScalafixPlugin)

lazy val testkit = project
  .in(file("scalafix-testkit"))
  .settings(
    moduleName := "scalafix-testkit",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      googleDiff,
      scalatest
    )
  )
  .dependsOn(cli)

lazy val unit = project
  .in(file("scalafix-tests/unit"))
  .settings(
    noPublishAndNoMima,
    // Change working directory to match when `fork := false`.
    Test / baseDirectory := (ThisBuild / baseDirectory).value,
    javaOptions := Nil,
    testFrameworks += new TestFramework("munit.Framework"),
    buildInfoPackage := "scalafix.tests",
    buildInfoObject := "BuildInfo",
    libraryDependencies ++= List(
      jgit,
      coursier,
      scalatest.withRevision(
        "3.2.0"
      ), // make sure testkit clients can use recent 3.x versions
      scalametaTeskit
    ),
    Compile / compile / compileInputs := {
      (Compile / compile / compileInputs)
        .dependsOn(
          testsInput / Compile / compile,
          testsOutput / Compile / compile,
          testsShared / Compile / compile
        )
        .value
    },
    Test / resourceGenerators += Def.task {
      // copy-pasted code from ScalafixTestkitPlugin to avoid cyclic dependencies between build and sbt-scalafix.
      val props = new java.util.Properties()
      def put(key: String, files: Seq[File]): Unit = {
        val value = files.iterator
          .filter(_.exists())
          .mkString(java.io.File.pathSeparator)
        props.put(key, value)
      }
      put(
        "inputClasspath",
        (testsInput / Compile / fullClasspath).value.map(_.data)
      )
      put(
        "inputSourceDirectories",
        (testsInput / Compile / sourceDirectories).value
      )
      put(
        "outputSourceDirectories",
        (testsOutput / Compile / sourceDirectories).value
      )
      props.put("scalaVersion", (testsInput / Compile / scalaVersion).value)
      props.put(
        "scalacOptions",
        (testsInput / Compile / scalacOptions).value.mkString("|")
      )
      val out =
        (Test / managedResourceDirectories).value.head /
          "scalafix-testkit.properties"
      IO.write(props, "Input data for scalafix testkit", out)
      List(out)
    },
    buildInfoKeys := Seq[BuildInfoKey](
      "scalametaVersion" -> scalametaV,
      "baseDirectory" ->
        (ThisBuild / baseDirectory).value,
      "inputSourceroot" ->
        (testsInput / Compile / sourceDirectory).value,
      "outputSourceroot" ->
        (testsOutput / Compile / sourceDirectory).value,
      "unitResourceDirectory" -> (Compile / resourceDirectory).value,
      "testsInputResources" ->
        (testsInput / Compile / sourceDirectory).value / "resources",
      "semanticClasspath" ->
        Seq(
          (testsInput / Compile / semanticdbTargetRoot).value,
          (testsShared / Compile / semanticdbTargetRoot).value
        ),
      "sharedSourceroot" ->
        (ThisBuild / baseDirectory).value /
        "scalafix-tests" / "shared" / "src" / "main",
      "sharedClasspath" ->
        (testsShared / Compile / classDirectory).value
    ),
    Test / test := (Test / test)
      .dependsOn(cli / crossPublishLocalBinTransitive)
      .value
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(testkit)

lazy val docs = project
  .in(file("scalafix-docs"))
  .settings(
    noPublishAndNoMima,
    run / baseDirectory := (ThisBuild / baseDirectory).value,
    moduleName := "scalafix-docs",
    scalaVersion := scala213,
    scalacOptions += "-Wconf:msg='match may not be exhaustive':s", // silence exhaustive pattern matching warning for documentation
    scalacOptions += "-Xfatal-warnings",
    mdoc := (Compile / run).evaluated,
    crossScalaVersions := List(scala213),
    libraryDependencies += (if (isScala211.value) metaconfigDocFor211
                            else metaconfigDoc)
  )
  .dependsOn(testkit, core, cli)
  .enablePlugins(DocusaurusPlugin)
  .disablePlugins(ScalafixPlugin)
