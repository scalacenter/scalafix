import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._
import Dependencies._

version.in(ThisBuild) ~= (_.replace('+', '-'))
name := "scalafixRoot"
onLoadMessage := s"Welcome to Scalafix ${version.value}"
noPublish

lazy val diff = crossProject
  .in(file("scalafix-diff"))
  .settings(
    moduleName := "scalafix-diff",
    description := "JVM/JS library to build unified diffs."
  )
  .jvmSettings(
    libraryDependencies += googleDiff
  )
  .jsSettings(
    allJSSettings,
    npmDependencies in Compile += "diff" -> "3.2.0"
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
lazy val diffJS = diff.js
lazy val diffJVM = diff.jvm

lazy val core = crossProject
  .in(file("scalafix-core"))
  .settings(
    moduleName := "scalafix-core",
    buildInfoSettings,
    libraryDependencies ++= List(
      scalameta.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  )
  .jvmSettings(
    libraryDependencies += "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  )
  .jsSettings(
    libraryDependencies += "com.geirsson" %%% "metaconfig-hocon" % metaconfigV
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(diff)
lazy val coreJS = core.js
lazy val coreJVM = core.jvm

lazy val reflect = project
  .configure(setId)
  .settings(
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
  .dependsOn(coreJVM)

lazy val cli = project
  .configure(setId)
  .settings(
    isFullCrossVersion,
    mainClass in assembly := Some("scalafix.cli.Cli"),
    assemblyJarName in assembly := "scalafix.jar",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "semanticdb-sbt-runtime" % semanticdbSbt,
      "com.github.alexarchambault" %% "case-app" % "1.1.3",
      "org.typelevel" %% "paiges-core" % "0.2.0",
      "com.martiansoftware" % "nailgun-server" % "0.9.1"
    )
  )
  .dependsOn(
    coreJVM,
    reflect,
    testkit % Test
  )

lazy val `scalafix-sbt` = project
  .configs(IntegrationTest)
  .settings(
    is210Only,
    Defaults.itSettings,
    buildInfoSettings,
    ScriptedPlugin.scriptedSettings,
    commands += Command.command(
      "installCompletions",
      "Code generates names of scalafix rules.",
      "") { s =>
      "cli/run --sbt scalafix-sbt/src/main/scala/scalafix/internal/sbt/ScalafixRewriteNames.scala" ::
        s
    },
    sbtPlugin := true,
    crossSbtVersions := Vector(sbt013, sbt1),
    libraryDependencies ++= coursierDeps,
    testQuick := {}, // these test are slow.
    test.in(IntegrationTest) := {
      RunSbtCommand(
        s"; plz $scala212 publishLocal " +
          "; very scalafix-sbt/scripted"
      )(state.value)
    },
    moduleName := "sbt-scalafix",
    mimaPreviousArtifacts := Set.empty,
    scriptedLaunchOpts ++= Seq(
      "-Dplugin.version=" + version.value,
      // .jvmopts is ignored, simulate here
      "-XX:MaxPermSize=256m",
      "-Xmx2g",
      "-Xss2m"
    ),
    scriptedBufferLog := false
  )
  .enablePlugins(BuildInfoPlugin)

lazy val testkit = project
  .configure(setId)
  .settings(
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      semanticdb,
      ammonite,
      googleDiff,
      scalatest.value
    )
  )
  .dependsOn(
    coreJVM,
    reflect
  )

lazy val testsShared = project
  .in(file("scalafix-tests/shared"))
  .settings(
    semanticdbSettings,
    noPublish
  )

lazy val testsInput = project
  .in(file("scalafix-tests/input"))
  .settings(
    noPublish,
    semanticdbSettings,
    scalacOptions += s"-P:semanticdb:sourceroot:${sourceDirectory.in(Compile).value}",
    scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
    scalacOptions += "-Ywarn-adapted-args", // For NoAutoTupling
    scalacOptions += "-Ywarn-unused-import", // For RemoveUnusedImports
    scalacOptions += "-Ywarn-unused", // For RemoveUnusedTerms
    logLevel := Level.Error, // avoid flood of compiler warnings
    // TODO: Remove once scala-xml-quote is merged into scala-xml
    resolvers += Resolver.bintrayRepo("allanrenucci", "maven"),
    libraryDependencies ++= testsDeps
  )
  .dependsOn(testsShared)

lazy val testsOutput = project
  .in(file("scalafix-tests/output"))
  .settings(
    noPublish,
    semanticdbSettings,
    scalacOptions -= warnUnusedImports,
    resolvers := resolvers.in(testsInput).value,
    libraryDependencies := libraryDependencies.in(testsInput).value
  )
  .dependsOn(testsShared)

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

lazy val testsInputSbt = project
  .in(file("scalafix-tests/input-sbt"))
  .settings(
    noPublish,
    logLevel := Level.Error, // avoid flood of deprecation warnings.
    scalacOptions += "-Xplugin-require:semanticdb-sbt",
    is210Only,
    sbtPlugin := true,
    scalacOptions += s"-P:semanticdb-sbt:sourceroot:${sourceDirectory.in(Compile).value}",
    addCompilerPlugin(
      "org.scalameta" % "semanticdb-sbt" % semanticdbSbt cross CrossVersion.full)
  )

lazy val testsOutputSbt = project
  .in(file("scalafix-tests/output-sbt"))
  .settings(
    noPublish,
    is210Only,
    sbtPlugin := true
  )

lazy val unit = project
  .in(file("scalafix-tests/unit"))
  .settings(
    noPublish,
    fork := false,
    javaOptions := Nil,
    buildInfoPackage := "scalafix.tests",
    buildInfoObject := "BuildInfo",
    sources.in(Test) +=
      sourceDirectory.in(`scalafix-sbt`, Compile).value /
        "scala" / "scalafix" / "internal" / "sbt" / "ScalafixJarFetcher.scala",
    compileInputs.in(Compile, compile) :=
      compileInputs
        .in(Compile, compile)
        .dependsOn(
          compile.in(testsInput, Compile),
          compile.in(testsInputSbt, Compile),
          compile.in(testsOutputSbt, Compile),
          compile.in(testsOutputDotty, Compile),
          compile.in(testsOutput, Compile)
        )
        .value,
    buildInfoKeys := Seq[BuildInfoKey](
      "baseDirectory" -> baseDirectory.in(ThisBuild).value,
      "inputSourceroot" ->
        sourceDirectory.in(testsInput, Compile).value,
      "inputSbtSourceroot" ->
        sourceDirectory.in(testsInputSbt, Compile).value,
      "outputSourceroot" ->
        sourceDirectory.in(testsOutput, Compile).value,
      "outputDottySourceroot" ->
        sourceDirectory.in(testsOutputDotty, Compile).value,
      "outputSbtSourceroot" ->
        sourceDirectory.in(testsOutputSbt, Compile).value,
      "testsInputResources" -> resourceDirectory.in(testsInput, Compile).value,
      "semanticSbtClasspath" -> classDirectory.in(testsInputSbt, Compile).value,
      "semanticClasspath" -> classDirectory.in(testsInput, Compile).value,
      "sharedClasspath" -> classDirectory.in(testsShared, Compile).value
    ),
    libraryDependencies ++= coursierDeps,
    libraryDependencies ++= testsDeps
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(
    testsInput,
    cli,
    testkit
  )

lazy val website = project
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    noPublish,
    websiteSettings,
    unidocSettings,
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(testkit, coreJVM)
  )
  .dependsOn(testkit, coreJVM, cli)
