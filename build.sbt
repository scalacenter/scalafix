import scalajsbundler.util.JSON._
import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._
import Dependencies._

inThisBuild(
  List(
    organization := "ch.epfl.scala",
    version := customScalafixVersion.getOrElse(version.value)
  )
)
name := {
  println(s"Welcome to scalafix ${version.value}")
  "scalafixRoot"
}

lazy val crossVersions = Seq(scala211, scala212)

// Custom scalafix release command. Tried sbt-release but didn't play well with sbt-doge.
commands += Command.command("release") { s =>
  "clean" ::
    "very publishSigned" ::
    "sonatypeRelease" ::
    "gitPushTag" ::
    s
}
commands += Command.command("ci-fast") { s =>
  "test" ::
    s
}
commands += Command.command("ci-slow") { s =>
  "scalafix-sbt/it:test" ::
    s
}

lazy val publishSettings = Seq(
  publishTo := Some(
    "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"),
  publishArtifact in Test := false,
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/scalacenter/scalafix")),
  autoAPIMappings := true,
  apiURL := Some(url("https://scalacenter.github.io/scalafix/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/scalacenter/scalafix"),
      "scm:git:git@github.com:scalacenter/scalafix.git"
    )
  ),
  mimaPreviousArtifacts := Set(
    organization.value %
      s"${moduleName.value}_${scalaBinaryVersion.value}" %
      sys.props.getOrElse("scalafix.stable.version", stableVersion.value)
  ),
  mimaBinaryIssueFilters ++= Mima.ignoredABIProblems,
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
)

lazy val noPublish = allSettings ++ Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val stableVersion =
  settingKey[String]("Version of latest release to Maven.")

lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    stableVersion,
    "coursier" -> coursier.util.Properties.version,
    "nightly" -> version.value,
    "scalameta" -> scalametaV,
    "semanticdbSbt" -> semanticdbSbt,
    scalaVersion,
    "supportedScalaVersions" -> Seq(scala211, scala212),
    "scala211" -> scala211,
    "scala212" -> scala212,
    sbtVersion
  ),
  buildInfoPackage := "scalafix",
  buildInfoObject := "Versions"
)

lazy val allSettings = List(
  version := sys.props.getOrElse("scalafix.version", version.value),
  stableVersion := version.value.replaceAll("\\+.*", ""),
  resolvers += Resolver.sonatypeRepo("releases"),
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions ++= compilerOptions.value,
  scalacOptions in (Compile, console) := compilerOptions.value :+ "-Yrepl-class-based",
  libraryDependencies += scalatest.value % Test,
  testOptions in Test += Tests.Argument("-oD"),
  scalaVersion := ciScalaVersion.getOrElse(scala212),
  crossScalaVersions := crossVersions,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val allJSSettings = List(
  additionalNpmConfig.in(Compile) := Map("private" -> bool(true))
)

allSettings

gitPushTag := {
  val tag = s"v${version.value}"
  assert(!tag.endsWith("SNAPSHOT"))
  import sys.process._
  Seq("git", "tag", "-a", tag, "-m", tag).!!
  Seq("git", "push", "--tags").!!
}

lazy val reflect = project
  .configure(setId)
  .settings(
    allSettings,
    publishSettings,
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
  .dependsOn(coreJVM)

lazy val core = crossProject
  .in(file("scalafix-core"))
  .settings(
    moduleName := "scalafix-core",
    allSettings,
    publishSettings,
    buildInfoSettings,
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
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

lazy val diff = crossProject
  .in(file("scalafix-diff"))
  .settings(
    moduleName := "scalafix-diff",
    allSettings,
    publishSettings
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

lazy val cli = project
  .configure(setId)
  .settings(
    allSettings,
    publishSettings,
    isFullCrossVersion,
    mainClass in assembly := Some("scalafix.cli.Cli"),
    assemblyJarName in assembly := "scalafix.jar",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "semanticdb-sbt-runtime" % semanticdbSbt,
      "com.github.alexarchambault" %% "case-app" % "1.1.3",
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
    allSettings,
    is210Only,
    publishSettings,
    buildInfoSettings,
    Defaults.itSettings,
    ScriptedPlugin.scriptedSettings,
    commands += Command.command(
      "installCompletions",
      "Code generates names of scalafix rewrites.",
      "") { s =>
      "cli/run --sbt scalafix-sbt/src/main/scala/scalafix/internal/sbt/ScalafixRewriteNames.scala" ::
        s
    },
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "io.get-coursier" %% "coursier" % coursier.util.Properties.version,
      "io.get-coursier" %% "coursier-cache" % coursier.util.Properties.version
    ),
    testQuick := {}, // these test are slow.
    test.in(IntegrationTest) := {
      RunSbtCommand(
        s"; plz $scala212 publishLocal " +
          "; very scalafix-sbt/scripted"
      )(state.value)
    },
    moduleName := "sbt-scalafix",
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
    allSettings,
    isFullCrossVersion,
    publishSettings,
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

lazy val testsDeps = List(
  // integration property tests
  "org.renucci" %% "scala-xml-quote" % "0.1.4",
  "org.typelevel" %% "catalysts-platform" % "0.0.5",
  "org.typelevel" %% "cats" % "0.9.0",
  "com.typesafe.slick" %% "slick" % "3.2.0-M2",
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scalacheck" %% "scalacheck" % "1.13.4"
)

lazy val semanticdbSettings = Seq(
  scalacOptions ++= List(
    "-Yrangepos",
    "-Xplugin-require:semanticdb"
  ),
  addCompilerPlugin(
    "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full)
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
    libraryDependencies ++= testsDeps
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(
    testsInput,
    cli,
    testkit
  )

lazy val integration = project
  .in(file("scalafix-tests/integration"))
  .configs(IntegrationTest)
  .settings(
    noPublish,
    Defaults.itSettings,
    test.in(IntegrationTest) := {
      test
        .in(IntegrationTest)
        .dependsOn(
          publishLocal.in(coreJVM),
          publishLocal.in(cli),
          publishLocal.in(reflect),
          publishLocal.in(`scalafix-sbt`)
        )
        .value
    },
    libraryDependencies += scalatest.value % IntegrationTest
  )
  .dependsOn(
    testsInput,
    coreJVM,
    reflect,
    testkit
  )

lazy val readme = scalatex
  .ScalatexReadme(
    projectId = "readme",
    wd = file(""),
    url = "https://github.com/scalacenter/scalafix/tree/master",
    source = "Readme")
  .settings(
    noPublish,
    git.remoteRepo := "git@github.com:scalacenter/scalafix.git",
    siteSourceDirectory := target.value / "scalatex",
    publish := {
      ghpagesPushSite
        .dependsOn(run.in(Compile).toTask(" --validate-links"))
        .value
    },
    scalacOptions ~= (_.filterNot(_ == "-Xlint")),
    test := run.in(Compile).toTask(" --validate-links").value,
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-eval" % "6.42.0"
    )
  )
  .dependsOn(coreJVM, cli)
  .enablePlugins(GhpagesPlugin)

lazy val is210Only = Seq(
  scalaVersion := scala210,
  crossScalaVersions := Seq(scala210),
  scalacOptions -= warnUnusedImports
)

lazy val isFullCrossVersion = Seq(
  crossVersion := CrossVersion.full
)

lazy val warnUnusedImports = "-Ywarn-unused-import"
lazy val compilerOptions = Def.setting {
  Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  )
}

lazy val gitPushTag = taskKey[Unit]("Push to git tag")

def setId(project: Project): Project = {
  val newId = "scalafix-" + project.id
  project
    .copy(base = file(newId))
    .settings(moduleName := newId)
}
def customScalafixVersion = sys.props.get("scalafix.version")

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
        password)).toSeq,
    PgpKeys.pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray())
  )
)
