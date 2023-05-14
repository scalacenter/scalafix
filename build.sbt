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
    libraryDependencies += metaconfig
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions)
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
  .jvmPlatform(buildScalaVersions)
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
  .jvmPlatform(buildScalaVersions)
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
      if (isScala3.value) Seq()
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
  .jvmPlatform(buildScalaVersions)
  .dependsOn(reflect, interfaces, rules)

lazy val testkit = projectMatrix
  .in(file("scalafix-testkit"))
  .settings(
    moduleName := "scalafix-testkit",
    isFullCrossVersion,
    libraryDependencies += googleDiff,
    libraryDependencies += scalatestDep.value
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions)
  .dependsOn(cli)

lazy val input = projectMatrix
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
  .jvmPlatform(buildScalaVersions)
  .disablePlugins(ScalafixPlugin)

lazy val output = projectMatrix
  .in(file("scalafix-tests/output"))
  .settings(
    noPublishAndNoMima,
    scalacOptions --= warnUnusedImports.value,
    libraryDependencies ++= testsDependencies.value,
    coverageEnabled := false
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions)
  .disablePlugins(ScalafixPlugin)

lazy val unit = projectMatrix
  .in(file("scalafix-tests/unit"))
  .settings(
    noPublishAndNoMima,
    libraryDependencies ++= List(
      jgit,
      munit,
      scalatest.withRevision(scalatestLatestV)
    ),
    libraryDependencies += {
      if (!isScala3.value) {
        scalametaTeskit
      } else {
        // exclude _2.13 artifacts that have their _3 counterpart in the classpath
        scalametaTeskit
          .exclude("com.lihaoyi", "sourcecode_2.13")
          .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
          .exclude("org.scalameta", "munit_2.13")
      }
    },
    buildInfoPackage := "scalafix.tests",
    buildInfoKeys := Seq[BuildInfoKey](
      "scalaVersion" -> scalaVersion.value
    )
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(testkit % Test)

lazy val integration = projectMatrix
  .in(file("scalafix-tests/integration"))
  .settings(
    noPublishAndNoMima,
    libraryDependencies += {
      if (!isScala3.value) {
        coursier
      } else {
        // exclude _2.13 artifacts that have their _3 counterpart in the classpath
        coursier
          .exclude("org.scala-lang.modules", "scala-xml_2.13")
          .exclude("org.scala-lang.modules", "scala-collection-compat_2.13")
      }
    },
    buildInfoPackage := "scalafix.tests",
    buildInfoObject := "BuildInfo",
    // create a local alias for input / Compile / fullClasspath at an
    // arbitrary, unused scope to be able to reference it (as a TaskKey) in
    // buildInfoKeys (since the macro only accepts TaskKeys)
    buildInfoKeys / fullClasspath :=
      resolve(input, Compile / fullClasspath).value,
    buildInfoKeys := Seq[BuildInfoKey](
      "scalametaVersion" -> scalametaV,
      "scalaVersion" -> scalaVersion.value,
      "baseDirectory" ->
        (ThisBuild / baseDirectory).value,
      "resourceDirectory" ->
        (Compile / resourceDirectory).value,
      "semanticClasspath" ->
        Seq((Compile / semanticdbTargetRoot).value),
      "sourceroot" ->
        (Compile / sourceDirectory).value,
      "classDirectory" ->
        (Compile / classDirectory).value,
      BuildInfoKey.map(buildInfoKeys / fullClasspath) { case (_, v) =>
        "inputClasspath" -> v
      },
      "inputSemanticClasspath" ->
        Seq(resolve(input, Compile / semanticdbTargetRoot).value),
      "inputSourceroot" ->
        resolve(input, Compile / sourceDirectory).value,
      "outputSourceroot" ->
        resolve(output, Compile / sourceDirectory).value
    ),
    Test / test := (Test / test)
      .dependsOn(cli.projectRefs.map(_ / publishLocalTransitive): _*)
      .value
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(buildScalaVersions)
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(unit % "compile->test")

lazy val expect = projectMatrix
  .in(file("scalafix-tests/expect"))
  .settings(
    noPublishAndNoMima,
    Test / resourceGenerators += Def.task {
      // make sure the output can be compiled
      val _ = TargetAxis.resolve(output, Compile / compile).value

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
          .resolve(input, Compile / fullClasspath)
          .value
          .map(_.data)
      )
      put(
        "inputSourceDirectories",
        TargetAxis
          .resolve(input, Compile / unmanagedSourceDirectories)
          .value
      )
      put(
        "outputSourceDirectories",
        TargetAxis
          .resolve(output, Compile / unmanagedSourceDirectories)
          .value
      )
      props.put(
        "scalaVersion",
        TargetAxis.resolve(input, Compile / scalaVersion).value
      )
      props.put(
        "scalacOptions",
        TargetAxis
          .resolve(input, Compile / scalacOptions)
          .value
          .mkString("|")
      )
      val out =
        (Test / managedResourceDirectories).value.head /
          "scalafix-testkit.properties"
      IO.write(props, "Input data for scalafix testkit", out)
      List(out)
    }
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(
    scalaVersions = Seq(scala3),
    axisValues = Seq(TargetAxis(scala3)),
    settings = Seq()
  )
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
  .dependsOn(integration)

lazy val docs = projectMatrix
  .in(file("scalafix-docs"))
  .settings(
    noPublishAndNoMima,
    run / baseDirectory := (ThisBuild / baseDirectory).value,
    moduleName := "scalafix-docs",
    scalacOptions += "-Wconf:msg='match may not be exhaustive':s", // silence exhaustive pattern matching warning for documentation
    scalacOptions += "-Xfatal-warnings",
    mdoc := (Compile / run).evaluated,
    libraryDependencies += metaconfigDoc
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = Seq(scala213))
  .dependsOn(testkit, core, cli)
  .enablePlugins(DocusaurusPlugin)
  .disablePlugins(ScalafixPlugin)
