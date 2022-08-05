import Dependencies._
import sbt.Keys.scalacOptions

inThisBuild(
  List(
    onLoadMessage := s"Welcome to scalafix ${version.value}",
    fork := true,
    semanticdbEnabled := true,
    semanticdbVersion := scalametaV,
    scalafixScalaBinaryVersion := "2.13",
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

lazy val interfaces = projectMatrix
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
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(false)
  .disablePlugins(ScalafixPlugin)

lazy val core = projectMatrix
  .in(file("scalafix-core"))
  .settings(
    moduleName := "scalafix-core",
    buildInfoSettingsForCore,
    libraryDependencies += googleDiff,
    libraryDependencies ++= {
      if (isScala3.value) {
        List(
          scalameta
            .exclude("com.lihaoyi", "sourcecode_2.13")
            .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
        )
      } else {
        List(
          scalameta,
          collectionCompat
        )
      }
    },
    libraryDependencies += {
      if (isScala211.value) metaconfigFor211
      else metaconfig
    }
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions :+ scala3)
  .enablePlugins(BuildInfoPlugin)

lazy val rules = projectMatrix
  .in(file("scalafix-rules"))
  .settings(
    moduleName := "scalafix-rules",
    description := "Built-in Scalafix rules",
    buildInfoSettingsForRules,
    libraryDependencies ++= {
      if (!isScala3.value)
        List(
          "org.scala-lang" % "scala-compiler" % scalaVersion.value,
          "org.scala-lang" % "scala-reflect" % scalaVersion.value,
          semanticdbScalacCore,
          collectionCompat
        )
      else Nil
    }
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions :+ scala3)
  .dependsOn(core)
  .enablePlugins(BuildInfoPlugin)

lazy val reflect = projectMatrix
  .in(file("scalafix-reflect"))
  .settings(
    moduleName := "scalafix-reflect",
    isFullCrossVersion,
    libraryDependencies ++= {
      if (!isScala3.value)
        List(
          "org.scala-lang" % "scala-compiler" % scalaVersion.value,
          "org.scala-lang" % "scala-reflect" % scalaVersion.value
        )
      else
        List(
          "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
          "org.scala-lang" %% "scala3-library" % scalaVersion.value
        )
    }
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions :+ scala3)
  .dependsOn(core)

lazy val cli = projectMatrix
  .in(file("scalafix-cli"))
  .settings(
    moduleName := "scalafix-cli",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      nailgunServer,
      jgit,
      commonText
    ),
    libraryDependencies ++= {
      if (!isScala3.value)
        Seq(java8Compat)
      else
        Seq()
    },
    libraryDependencies ++= {
      if (isScala211.value || isScala3.value) Seq()
      else
        Seq(
          // metaconfig 0.10.0 shaded pprint but rules built with an old
          // scalafix-core must have the original package in the classpath to link
          // https://github.com/scalameta/metaconfig/pull/154/files#r794005161
          pprint % Runtime
        )
    },
    publishLocalTransitive := Def.taskDyn {
      val ref = thisProjectRef.value
      publishLocal.all(ScopeFilter(inDependencies(ref)))
    }.value
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions :+ scala3)
  .dependsOn(reflect, interfaces, rules)

lazy val testsShared = projectMatrix
  .in(file("scalafix-tests/shared"))
  .settings(
    noPublishAndNoMima,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(testTargetScalaVersions)
  .disablePlugins(ScalafixPlugin)

lazy val testsInput = projectMatrix
  .in(file("scalafix-tests/input"))
  .settings(
    noPublishAndNoMima,
    scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
    scalacOptions ++= warnAdaptedArgs.value, // For NoAutoTupling
    scalacOptions ++= warnUnusedImports.value, // For RemoveUnused
    scalacOptions ++= warnUnused.value, // For RemoveUnusedTerms
    logLevel := Level.Error, // avoid flood of compiler warnings
    libraryDependencies ++= testsDependencies.value,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(testTargetScalaVersions)
  .disablePlugins(ScalafixPlugin)

lazy val testsOutput = projectMatrix
  .in(file("scalafix-tests/output"))
  .settings(
    noPublishAndNoMima,
    scalacOptions --= warnUnusedImports.value,
    libraryDependencies ++= testsDependencies.value,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(testTargetScalaVersions)
  .disablePlugins(ScalafixPlugin)

lazy val testkit = projectMatrix
  .in(file("scalafix-testkit"))
  .settings(
    moduleName := "scalafix-testkit",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      googleDiff,
      scalatest
    )
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions)
  .dependsOn(cli)

lazy val unit = projectMatrix
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
          TargetAxis.resolve(testsInput, Compile / compile),
          TargetAxis.resolve(testsOutput, Compile / compile),
          TargetAxis.resolve(testsShared, Compile / compile)
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
        TargetAxis
          .resolve(testsInput, Compile / fullClasspath)
          .value
          .map(_.data)
      )
      put(
        "inputSourceDirectories",
        TargetAxis
          .resolve(testsInput, Compile / unmanagedSourceDirectories)
          .value
      )
      put(
        "outputSourceDirectories",
        TargetAxis
          .resolve(testsOutput, Compile / unmanagedSourceDirectories)
          .value
      )
      props.put(
        "scalaVersion",
        TargetAxis.resolve(testsInput, Compile / scalaVersion).value
      )
      props.put(
        "scalacOptions",
        TargetAxis
          .resolve(testsInput, Compile / scalacOptions)
          .value
          .mkString("|")
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
      "unitResourceDirectory" -> (Compile / resourceDirectory).value,
      "semanticClasspath" ->
        Seq(
          TargetAxis.resolve(testsInput, Compile / semanticdbTargetRoot).value,
          TargetAxis.resolve(testsShared, Compile / semanticdbTargetRoot).value
        ),
      "sharedSourceroot" ->
        (ThisBuild / baseDirectory).value /
        "scalafix-tests" / "shared" / "src" / "main",
      "sharedClasspath" ->
        TargetAxis.resolve(testsShared, Compile / classDirectory).value
    ),
    Test / test := (Test / test)
      .dependsOn(cli.projectRefs.map(_ / publishLocalTransitive): _*)
      .value,
    Test / unmanagedSourceDirectories ++= {
      val sourceDir = (Test / sourceDirectory).value
      val maybeTargetScalaVersion =
        TargetAxis
          .targetScalaVersion(virtualAxes.value)
          .flatMap(CrossVersion.partialVersion(_))
      maybeTargetScalaVersion match {
        case Some((n, m)) =>
          Seq(
            sourceDir / s"scala-target$n",
            sourceDir / s"scala-target$n.$m"
          )
        case _ => Seq()
      }
    }
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(
    scalaVersions = Seq(scala212),
    axisValues = Seq(TargetAxis(scala3)),
    settings = Seq()
  )
  .jvmPlatform(
    scalaVersions = Seq(scala213),
    axisValues = Seq(TargetAxis(scala213)),
    settings = Seq()
  )
  .jvmPlatform(
    scalaVersions = Seq(scala212),
    axisValues = Seq(TargetAxis(scala212)),
    settings = Seq()
  )
  .jvmPlatform(
    scalaVersions = Seq(scala211),
    axisValues = Seq(TargetAxis(scala211)),
    settings = Seq()
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(testkit)

lazy val docs = projectMatrix
  .in(file("scalafix-docs"))
  .settings(
    noPublishAndNoMima,
    run / baseDirectory := (ThisBuild / baseDirectory).value,
    moduleName := "scalafix-docs",
    scalacOptions += "-Wconf:msg='match may not be exhaustive':s", // silence exhaustive pattern matching warning for documentation
    scalacOptions += "-Xfatal-warnings",
    mdoc := (Compile / run).evaluated,
    libraryDependencies += (if (isScala211.value) metaconfigDocFor211
                            else metaconfigDoc)
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = Seq(scala213))
  .dependsOn(testkit, core, cli)
  .enablePlugins(DocusaurusPlugin)
  .disablePlugins(ScalafixPlugin)
