import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._
import Dependencies._
import xsbti.Position
import xsbti.Reporter
import xsbti.Severity
organization in ThisBuild := "ch.epfl.scala"
version in ThisBuild := customScalafixVersion.getOrElse(
  version.in(ThisBuild).value)

lazy val crossVersions = Seq(scala211, scala212)

// Custom scalafix release command. Tried sbt-release but didn't play well with sbt-doge.
commands += Command.command("release") { s =>
  "clean" ::
    "very publishSigned" ::
    "sonatypeRelease" ::
    "gitPushTag" ::
    s
}
commands += CiCommand("ci-fast")(
  ci("test") ::
    "such testsOutputDotty/test" ::
    Nil
)
commands += Command.command("ci-slow") { s =>
  "scalafix-sbt/it:test" ::
    s
}
commands += Command.command("ci-publish") { s =>
  s"very publish" :: s
}

lazy val publishSettings = Seq(
  publishTo := {
    if (customScalafixVersion.isDefined)
      Some(
        "releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
    else
      publishTo.in(bintray).value
  },
  bintrayOrganization := Some("scalameta"),
  bintrayRepository := "maven",
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
  pomExtra :=
    <developers>
      <developer>
        <id>olafurpg</id>
        <name>Ólafur Páll Geirsson</name>
        <url>https://geirsson.com</url>
      </developer>
    </developers>
)

lazy val noPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val buildInfoSettings: Seq[Def.Setting[_]] = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    "stable" -> version.value.replaceAll("\\+.*", ""),
    "nightly" -> version.value,
    "scalameta" -> scalametaV,
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
  resolvers += Resolver.bintrayRepo("scalameta", "maven"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers ~= { old =>
    if (isDroneCI) {
      println(s"Using resolver: $epflArtifactory")
      epflArtifactory +: old
    } else old
  },
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions ++= compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  libraryDependencies += scalatest % Test,
  testOptions in Test += Tests.Argument("-oD"),
  scalaVersion := ciScalaVersion.getOrElse(scala212),
  crossScalaVersions := crossVersions,
  scalametaSemanticdb := ScalametaSemanticdb.Disabled,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

allSettings

gitPushTag := {
  val tag = s"v${version.value}"
  assert(!tag.endsWith("SNAPSHOT"))
  import sys.process._
  Seq("git", "tag", "-a", tag, "-m", tag).!!
  Seq("git", "push", "--tags").!!
}

// settings to projects using @metaconfig.ConfDecoder annotation.
lazy val metaconfigSettings: Seq[Def.Setting[_]] = Seq(
  addCompilerPlugin(
    ("org.scalameta" % "paradise" % paradiseV).cross(CrossVersion.full)),
  scalacOptions += "-Xplugin-require:macroparadise",
  scalacOptions in (Compile, console) := Seq(), // macroparadise plugin doesn't work in repl yet.
  sources in (Compile, doc) := Nil // macroparadise doesn't work with scaladoc yet.
)

lazy val reflect = project
  .configure(setId)
  .settings(
    allSettings,
    publishSettings,
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value
    )
  )
  .dependsOn(core)

lazy val core = project
  .configure(setId)
  .settings(
    allSettings,
    publishSettings,
    buildInfoSettings,
    metaconfigSettings,
    dependencyOverrides += scalameta,
    libraryDependencies ++= Seq(
      metaconfig,
      scalameta,
      // TODO(olafur) can we move this into separate module.
      // Currently only used in Patch.appliedDiff
      googleDiff
    )
  )
  .enablePlugins(BuildInfoPlugin)

lazy val cli = project
  .configure(setId)
  .settings(
    allSettings,
    publishSettings,
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      "com.github.alexarchambault" %% "case-app"      % "1.1.3",
      "com.martiansoftware"        % "nailgun-server" % "0.9.1"
    )
  )
  .dependsOn(
    core,
    reflect,
    testkit % Test
  )

lazy val `scalafix-sbt` = project
  .configs(IntegrationTest)
  .settings(
    allSettings,
    publishSettings,
    buildInfoSettings,
    Defaults.itSettings,
    ScriptedPlugin.scriptedSettings,
    sbtPlugin := true,
    testQuick := {}, // these test are slow.
    test.in(IntegrationTest) := {
      RunSbtCommand(
        s"; plz $scala212 publishLocal " +
          "; very scalafix-sbt/scripted"
      )(state.value)
    },
    addSbtPlugin(scalahostSbt),
    scalaVersion := scala210,
    crossScalaVersions := Seq(scala210),
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
      scalahost,
      ammonite,
      googleDiff,
      scalatest
    )
  )
  .dependsOn(
    core,
    reflect
  )

lazy val testsDeps = List(
  // integration property tests
  "org.renucci"        %% "scala-xml-quote"    % "0.1.4",
  "org.typelevel"      %% "catalysts-platform" % "0.0.5",
  "com.typesafe.slick" %% "slick"              % "3.2.0-M2",
  "com.chuusai"        %% "shapeless"          % "2.3.2",
  "org.scalacheck"     %% "scalacheck"         % "1.13.4"
)

lazy val testsShared = project
  .in(file("scalafix-tests/shared"))
  .settings(
    allSettings,
    noPublish
  )

lazy val testsInput = project
  .in(file("scalafix-tests/input"))
  .settings(
    allSettings,
    noPublish,
    scalametaSourceroot := sourceDirectory.in(Compile).value,
    scalametaSemanticdb := ScalametaSemanticdb.Fat,
    scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
    scalacOptions += "-Ywarn-adapted-args", // For NoAutoTupling
    scalacOptions += "-Ywarn-unused-import", // For RemoveUnusedImports
    // TODO: Remove once scala-xml-quote is merged into scala-xml
    resolvers += Resolver.bintrayRepo("allanrenucci", "maven"),
    libraryDependencies ++= testsDeps
  )
  .dependsOn(testsShared)

lazy val testsOutput = project
  .in(file("scalafix-tests/output"))
  .settings(
    allSettings,
    noPublish,
    resolvers := resolvers.in(testsInput).value,
    libraryDependencies := libraryDependencies.in(testsInput).value
  )
  .dependsOn(testsShared)

lazy val testsOutputDotty = project
  .in(file("scalafix-tests/output-dotty"))
  .settings(
    allSettings,
    noPublish,
    scalaVersion := dotty,
    crossScalaVersions := List(dotty),
    libraryDependencies := libraryDependencies.value.map(_.withDottyCompat()),
    scalacOptions := Nil
  )
  .disablePlugins(ScalahostSbtPlugin)

lazy val unit = project
  .in(file("scalafix-tests/unit"))
  .settings(
    allSettings,
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
          compile.in(testsOutput, Compile)
        )
        .value,
    buildInfoKeys := Seq[BuildInfoKey](
      "inputSourceroot" ->
        sourceDirectory.in(testsInput, Compile).value,
      "outputSourceroot" ->
        sourceDirectory.in(testsOutput, Compile).value,
      "outputDottySourceroot" ->
        sourceDirectory.in(testsOutputDotty, Compile).value,
      "testsInputResources" -> resourceDirectory.in(testsInput, Compile).value,
      "mirrorClasspath" -> classDirectory.in(testsInput, Compile).value
    ),
    libraryDependencies ++= testsDeps
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(
    testsInput % Scalameta,
    cli,
    testkit
  )

lazy val integration = project
  .in(file("scalafix-tests/integration"))
  .configs(IntegrationTest)
  .settings(
    allSettings,
    noPublish,
    Defaults.itSettings,
    test.in(IntegrationTest) := {
      test
        .in(IntegrationTest)
        .dependsOn(
          publishLocal.in(core),
          publishLocal.in(cli),
          publishLocal.in(reflect),
          publishLocal.in(`scalafix-sbt`)
        )
        .value
    },
    libraryDependencies += scalatest % IntegrationTest
  )
  .dependsOn(
    testsInput % Scalameta,
    core,
    reflect,
    testkit
  )

lazy val readme = scalatex
  .ScalatexReadme(projectId = "readme",
                  wd = file(""),
                  url = "https://github.com/scalacenter/scalafix/tree/master",
                  source = "Readme")
  .settings(
    allSettings,
    noPublish,
    git.remoteRepo := "git@github.com:scalacenter/scalafix.git",
    siteSourceDirectory := target.value / "scalatex",
    publish := {
      ghpagesPushSite
        .dependsOn(run.in(Compile).toTask(" --validate-links"))
        .value
    },
    test := run.in(Compile).toTask(" --validate-links").value,
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-eval" % "6.42.0"
    )
  )
  .dependsOn(core, cli)
  .enablePlugins(GhpagesPlugin)

lazy val isFullCrossVersion = Seq(
  crossVersion := CrossVersion.full
)

lazy val dotty = "0.1.1-bin-20170530-f8f52cc-NIGHTLY"
lazy val scala210 = "2.10.6"
lazy val scala211 = "2.11.11"
lazy val scala212 = "2.12.2"
lazy val ciScalaVersion = sys.env.get("CI_SCALA_VERSION")
def CiCommand(name: String)(commands: List[String]): Command =
  Command.command(name) { initState =>
    commands.foldLeft(initState) {
      case (state, command) => command :: state
    }
  }
def ci(command: String) =
  s"plz ${ciScalaVersion.getOrElse("No CI_SCALA_VERSION defined")} $command"

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-language:existentials",
  //  "-Ywarn-numeric-widen", // TODO(olafur) enable
  "-Xfuture",
  "-Xlint"
)

lazy val gitPushTag = taskKey[Unit]("Push to git tag")

def setId(project: Project): Project = {
  val newId = "scalafix-" + project.id
  project
    .copy(base = file(newId))
    .settings(moduleName := newId)
    .disablePlugins(ScalahostSbtPlugin)
}
def customScalafixVersion = sys.props.get("scalafix.version")
def isDroneCI = sys.env.get("CI").exists(_ == "DRONE")
def epflArtifactory =
  MavenRepository("epfl-artifactory",
                  "http://scala-webapps.epfl.ch:8081/artifactory/dbuild/")
