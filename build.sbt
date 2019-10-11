import Dependencies._
inThisBuild(
  List(
    onLoadMessage := s"Welcome to scalafix ${version.value}",
    scalaVersion := scala212,
    crossScalaVersions := List(scala212, scala211),
    fork.in(Test, test) := true
  )
)

cancelable.in(Global) := true

noPublish

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
    noMima,
    resourceGenerators.in(Compile) += Def.task {
      val props = new java.util.Properties()
      props.put("scalafixVersion", version.value)
      props.put("scalafixStableVersion", stableVersion.value)
      props.put("scalametaVersion", scalametaV)
      props.put("scala212", scala212)
      props.put("scala211", scala211)
      props.put("scala213", scala213)
      val out =
        managedResourceDirectories.in(Compile).value.head /
          "scalafix-interfaces.properties"
      IO.write(props, "Scalafix version constants", out)
      List(out)

    },
    javacOptions.in(Compile) ++= List(
      "-Xlint:all",
      "-Werror"
    ),
    javacOptions.in(Compile, doc) := List("-Xdoclint:none"),
    javaHome.in(Compile) := inferJavaHome(),
    javaHome.in(Compile, doc) := inferJavaHome(),
    moduleName := "scalafix-interfaces",
    crossVersion := CrossVersion.disabled,
    crossScalaVersions := List(scala212),
    autoScalaLibrary := false
  )

lazy val core = project
  .in(file("scalafix-core"))
  .settings(
    moduleName := "scalafix-core",
    buildInfoSettings,
    libraryDependencies ++= List(
      scalameta,
      googleDiff,
      "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  )
  .enablePlugins(BuildInfoPlugin)

lazy val rules = project
  .in(file("scalafix-rules"))
  .settings(
    moduleName := "scalafix-rules",
    description := "Built-in Scalafix rules",
    libraryDependencies += "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
  )
  .dependsOn(core)

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
  .dependsOn(core, rules)

lazy val cli = project
  .in(file("scalafix-cli"))
  .settings(
    moduleName := "scalafix-cli",
    isFullCrossVersion,
    mainClass in assembly := Some("scalafix.v1.Main"),
    assemblyJarName in assembly := "scalafix.jar",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
      "com.martiansoftware" % "nailgun-server" % "0.9.1",
      jgit,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.apache.commons" % "commons-text" % "1.2"
    )
  )
  .dependsOn(reflect, interfaces)

lazy val testsShared = project
  .in(file("scalafix-tests/shared"))
  .settings(
    semanticdbSettings,
    noPublish,
    coverageEnabled := false
  )

val isScala213 = Def.setting(scalaVersion.value.startsWith("2.13"))

val warnAdaptedArgs = Def.setting {
  if (isScala213.value) "-Xlint:adapted-args"
  else "-Ywarn-adapted-args"
}

lazy val testsInput = project
  .in(file("scalafix-tests/input"))
  .settings(
    noPublish,
    semanticdbSettings,
    scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
    scalacOptions += warnAdaptedArgs.value, // For NoAutoTupling
    scalacOptions += warnUnusedImports.value, // For RemoveUnused
    scalacOptions += "-Ywarn-unused", // For RemoveUnusedTerms
    logLevel := Level.Error, // avoid flood of compiler warnings
    libraryDependencies += "com.twitter" %% "bijection-core" % "0.9.5",
    testsInputOutputSetting,
    coverageEnabled := false
  )

lazy val testsOutput = project
  .in(file("scalafix-tests/output"))
  .settings(
    noPublish,
    semanticdbSettings,
    scalacOptions --= List(
      warnUnusedImports.value,
      "-Xlint"
    ),
    testsInputOutputSetting,
    coverageEnabled := false,
    libraryDependencies += "com.twitter" %% "bijection-core" % "0.9.5"
  )

lazy val testkit = project
  .in(file("scalafix-testkit"))
  .settings(
    noMima,
    moduleName := "scalafix-testkit",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      semanticdb,
      googleDiff,
      scalacheck,
      scalatest
    )
  )
  .dependsOn(cli)

lazy val unit = project
  .in(file("scalafix-tests/unit"))
  .settings(
    noPublish,
    fork := false,
    javaOptions := Nil,
    buildInfoPackage := "scalafix.tests",
    buildInfoObject := "BuildInfo",
    libraryDependencies ++= testsDeps,
    libraryDependencies ++= List(
      jgit,
      semanticdbPluginLibrary,
      scalatest,
      "com.lihaoyi" %% "ujson" % "0.8.0",
      "org.scalameta" %% "testkit" % scalametaV
    ),
    compileInputs.in(Compile, compile) := {
      compileInputs
        .in(Compile, compile)
        .dependsOn(
          compile.in(testsInput, Compile),
          compile.in(testsOutput, Compile)
        )
        .value
    },
    resourceGenerators.in(Test) += Def.task {
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
        fullClasspath.in(testsInput, Compile).value.map(_.data)
      )
      put(
        "inputSourceDirectories",
        sourceDirectories.in(testsInput, Compile).value
      )
      put(
        "outputSourceDirectories",
        sourceDirectories.in(testsOutput, Compile).value
      )
      props.put("scalaVersion", scalaVersion.value)
      props.put("scalacOptions", scalacOptions.value.mkString("|"))
      val out =
        managedResourceDirectories.in(Test).value.head /
          "scalafix-testkit.properties"
      IO.write(props, "Input data for scalafix testkit", out)
      List(out)
    },
    buildInfoKeys := Seq[BuildInfoKey](
      "baseDirectory" ->
        baseDirectory.in(ThisBuild).value,
      "inputSourceroot" ->
        sourceDirectory.in(testsInput, Compile).value,
      "outputSourceroot" ->
        sourceDirectory.in(testsOutput, Compile).value,
      "unitResourceDirectory" -> resourceDirectory.in(Compile).value,
      "testsInputResources" ->
        sourceDirectory.in(testsInput, Compile).value / "resources",
      "semanticClasspath" ->
        classDirectory.in(testsInput, Compile).value,
      "sharedSourceroot" ->
        baseDirectory.in(ThisBuild).value /
          "scalafix-tests" / "shared" / "src" / "main",
      "sharedClasspath" ->
        classDirectory.in(testsShared, Compile).value
    )
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(
    testsInput,
    testsShared,
    cli,
    testkit
  )

lazy val docs = project
  .in(file("scalafix-docs"))
  .settings(
    noMima,
    skip in publish := true,
    moduleName := "scalafix-docs",
    scalaVersion := scala212,
    mdoc := run.in(Compile).evaluated,
    crossScalaVersions := List(scala212),
    libraryDependencies ++= List(
      "com.geirsson" %% "metaconfig-docs" % metaconfigV,
      "org.scalameta" % "semanticdb-scalac-core" % scalametaV cross CrossVersion.full
    )
  )
  .dependsOn(testkit, core, cli)
  .enablePlugins(DocusaurusPlugin)
