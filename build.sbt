import Dependencies._

version.in(ThisBuild) ~= { old: String =>
  val suffix =
    if (sys.props.contains("scalafix.snapshot")) "-SNAPSHOT"
    else ""
  sys.props.getOrElse("scalafix.version", old.replace('+', '-') + suffix)
}

lazy val scalaFixedProjects: List[ProjectReference] =
  List(
    testsOutputDotty,
    website
  )

lazy val scala212Projects: List[ProjectReference] =
  List(
    cli212,
    core212JS,
    core212JVM,
    diff212JS,
    diff212JVM,
    reflect212,
    testkit212,
    testsInput212,
    testsOutput212,
    testsShared212,
    unit212
  )

lazy val allScala212Projects: List[ProjectReference] =
  scala212Projects ++ scalaFixedProjects

lazy val scala212ProjectsDependencies: List[ClasspathDep[ProjectReference]] =
  scala212Projects.map(ClasspathDependency(_, None))

lazy val scala211Projects: List[ProjectReference] =
  List(
    cli211,
    core211JS,
    core211JVM,
    diff211JS,
    diff211JVM,
    reflect211,
    testkit211,
    testsInput211,
    testsOutput211,
    testsShared211,
    unit211
  )

lazy val allScala211Projects: List[ProjectReference] =
  scala211Projects ++ scalaFixedProjects

lazy val scala211ProjectsDependencies: List[ClasspathDep[ProjectReference]] =
  scala211Projects.map(ClasspathDependency(_, None))

lazy val scalafix = project
  .in(file("."))
  .settings(
    moduleName := "scalafixRoot",
    onLoadMessage := s"Welcome to Scalafix ${version.value}",
    noPublish,
    scalaVersion := scala212
  )
  .aggregate(allScala212Projects: _*)
  .dependsOn(scala212ProjectsDependencies: _*)

lazy val scalafix211 = project
  .in(file(".scalafix211"))
  .settings(
    moduleName := "scalafix211",
    noPublish,
    scalaVersion := scala211
  )
  .aggregate(allScala211Projects: _*)
  .dependsOn(scala211ProjectsDependencies: _*)

val diff = MultiScalaCrossProject(
  "diff",
  _.settings(
    moduleName := "scalafix-diff",
    description := "JVM/JS library to build unified diffs."
  ).jvmSettings(
      libraryDependencies += googleDiff
    )
    .jsSettings(
      allJSSettings,
      npmDependencies in Compile += "diff" -> "3.2.0"
    )
    .jsConfigure(
      _.enablePlugins(ScalaJSBundlerPlugin)
    )
)

val diff211 = diff(scala211)
val diff212 = diff(scala212)

lazy val diff211JVM = diff211.jvm
lazy val diff211JS = diff211.js
lazy val diff212JVM = diff212.jvm
lazy val diff212JS = diff212.js

val core = MultiScalaCrossProject(
  "core",
  _.settings(
    buildInfoSettings,
    libraryDependencies ++= List(
      scalameta.value,
      symtab,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  ).jvmSettings(
      libraryDependencies += "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
    )
    .jsSettings(
      libraryDependencies += "com.geirsson" %%% "metaconfig-hocon" % metaconfigV
    )
    .enablePlugins(BuildInfoPlugin)
)

val core211 = core(scala211, _.dependsOn(diff211))
val core212 = core(scala212, _.dependsOn(diff212))

lazy val core211JVM = core211.jvm
lazy val core211JS = core211.js
lazy val core212JVM = core212.jvm
lazy val core212JS = core212.js

val reflect = MultiScalaProject(
  "reflect",
  _.settings(
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      metacp,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
)

lazy val reflect211 = reflect(scala211, _.dependsOn(core211JVM))
lazy val reflect212 = reflect(scala212, _.dependsOn(core212JVM))

val cli = MultiScalaProject(
  "cli",
  _.settings(
    isFullCrossVersion,
    mainClass in assembly := Some("scalafix.v1.Main"),
    assemblyJarName in assembly := "scalafix.jar",
    libraryDependencies ++= Seq(
      "com.martiansoftware" % "nailgun-server" % "0.9.1",
      jgit,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.apache.commons" % "commons-text" % "1.2"
    )
  )
)
lazy val cli211 =
  cli(scala211, _.dependsOn(core211JVM, reflect211, testkit211 % Test))
lazy val cli212 =
  cli(scala212, _.dependsOn(core212JVM, reflect212, testkit212 % Test))

val testUtils = MultiScalaProject(
  "test-utils",
  _.settings(
    libraryDependencies ++= Seq(
      jgit,
      scalatest
    )
  )
)
lazy val testUtils210 = testUtils(
  scala210,
  _.settings(
    scalacOptions -= warnUnusedImports
  )
)
lazy val testUtils211 = testUtils(scala211)
lazy val testUtils212 = testUtils(scala212)

val testkit = MultiScalaProject(
  "testkit",
  _.settings(
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      semanticdb,
      googleDiff,
      scalacheck,
      scalatest
    )
  )
)

lazy val testkit211 =
  testkit(scala211, _.dependsOn(core211JVM, reflect211))
lazy val testkit212 =
  testkit(scala212, _.dependsOn(core212JVM, reflect212))

val testsShared = TestProject(
  "shared",
  _.settings(
    semanticdbSettings,
    noPublish
  )
)

lazy val testsShared211 = testsShared(scala211)
lazy val testsShared212 = testsShared(scala212)

val testsInput = TestProject(
  "input",
  (project, srcMain) =>
    project.settings(
      noPublish,
      semanticdbSettings,
      scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
      scalacOptions += "-Ywarn-adapted-args", // For NoAutoTupling
      scalacOptions += "-Ywarn-unused-import", // For RemoveUnusedImports
      scalacOptions += "-Ywarn-unused", // For RemoveUnusedTerms
      logLevel := Level.Error, // avoid flood of compiler warnings
      testsInputOutputSetting
  )
)

lazy val testsInput211 = testsInput(scala211, _.dependsOn(testsShared211))
lazy val testsInput212 = testsInput(scala212, _.dependsOn(testsShared212))

val testsOutput = TestProject(
  "output",
  (project, srcMain) =>
    project.settings(
      noPublish,
      semanticdbSettings,
      unmanagedSourceDirectories.in(Compile) +=
        baseDirectory.in(ThisBuild).value / srcMain /
          s"scala-${scalaBinaryVersion.value}",
      scalacOptions --= List(
        warnUnusedImports,
        "-Xlint"
      ),
      testsInputOutputSetting
  )
)

val testsOutput211 = testsOutput(scala211, _.dependsOn(testsShared211))
val testsOutput212 = testsOutput(scala212, _.dependsOn(testsShared212))

lazy val testsOutputDotty = project
  .in(file("scalafix-tests/output-dotty"))
  .settings(
    noPublish,
    // Skip this project for IntellIJ, see https://youtrack.jetbrains.com/issue/SCL-12237
    SettingKey[Boolean]("ide-skip-project") := true,
    scalaVersion := dotty,
    crossScalaVersions := List(dotty),
    libraryDependencies := libraryDependencies.value.map(_.withDottyCompat()),
    scalacOptions := Nil
  )

def unit(
    scalav: String,
    cli: Project,
    testkit: Project,
    testUtils: Project,
    testsInput: Project,
    testsInputMulti: MultiScalaProject,
    testsOutput: Project,
    testsOutputMulti: MultiScalaProject,
    testsOutputDotty: Project,
    testsShared: Project
): Project = {

  val unitMultiProject =
    MultiScalaProject(
      "unit",
      s"scalafix-tests/unit",
      _.settings(
        noPublish,
        fork := false,
        javaOptions := Nil,
        buildInfoPackage := "scalafix.tests",
        buildInfoObject := "BuildInfo",
        libraryDependencies ++= coursierDeps ++ testsDeps
      ).enablePlugins(BuildInfoPlugin)
    )

  unitMultiProject(
    scalav,
    _.settings(
      compileInputs.in(Compile, compile) := {
        compileInputs
          .in(Compile, compile)
          .dependsOn(
            compile.in(testsInput, Compile),
            compile.in(testsOutputDotty, Compile),
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
          val inputScalaVersion = scalaBinaryVersion.in(testsInput).value
          val outputScalaVersion = scalaBinaryVersion.value
          val adjustedValue = value.replace(
            s"scala-$inputScalaVersion",
            s"scala-$outputScalaVersion"
          )
          props.put(key, adjustedValue)
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
          sourceDirectories.in(testsOutput, Compile).value ++
            sourceDirectories.in(testsOutputDotty, Compile).value
        )
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
          baseDirectory.in(ThisBuild).value / testsInputMulti.srcMain,
        "outputSourceroot" ->
          baseDirectory.in(ThisBuild).value / testsOutputMulti.srcMain,
        "testsInputResources" ->
          baseDirectory
            .in(ThisBuild)
            .value / testsInputMulti.srcMain / "resources",
        "outputDottySourceroot" ->
          sourceDirectory.in(testsOutputDotty, Compile).value,
        "semanticClasspath" ->
          classDirectory.in(testsInput, Compile).value,
        "sharedSourceroot" ->
          baseDirectory.in(ThisBuild).value /
            "scalafix-tests" / "shared" / "src" / "main",
        "sharedClasspath" ->
          classDirectory.in(testsShared, Compile).value
      )
    ).dependsOn(
      testsInput,
      cli,
      testkit,
      testUtils
    )
  )
}

lazy val unit211 = unit(
  scala211,
  cli211,
  testkit211,
  testUtils211,
  testsInput211,
  testsInput,
  testsOutput211,
  testsOutput,
  testsOutputDotty,
  testsShared211
)

lazy val unit212 = unit(
  scala212,
  cli212,
  testkit212,
  testUtils212,
  testsInput212,
  testsInput,
  testsOutput212,
  testsOutput,
  testsOutputDotty,
  testsShared212
)

lazy val website = project
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    scalaVersion := scala212,
    noPublish,
    websiteSettings,
    unidocSettings,
    libraryDependencies += "com.geirsson" %% "metaconfig-docs" % metaconfigV,
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(
      testkit212,
      core212JVM
    )
  )
  .dependsOn(testkit212, core212JVM, cli212)

inScope(Global)(
  Seq(
    credentials ++= (for {
      username <- sys.env.get("SONATYPE_USERNAME")
      password <- sys.env.get("SONATYPE_PASSWORD")
    } yield
      Credentials(
        "Sonatype Nexus Repository Manager",
        "oss.sonatype.org",
        username,
        password
      )).toSeq,
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
