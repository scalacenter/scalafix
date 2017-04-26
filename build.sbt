import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._

import Dependencies._
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
commands += CiCommand("ci-fast")("test" :: Nil)
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
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions := compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  libraryDependencies += scalatest % Test,
  testOptions in Test += Tests.Argument("-oD"),
  scalaVersion := ciScalaVersion.getOrElse(scala211),
  crossScalaVersions := crossVersions,
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
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )
  .dependsOn(core)
lazy val core = project
  .configure(setId)
  .settings(
    allSettings,
    publishSettings,
    buildInfoSettings,
    metaconfigSettings,
    isFullCrossVersion,
    dependencyOverrides += scalameta,
    libraryDependencies ++= Seq(
      metaconfig,
      scalameta,
      scalahost,
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

lazy val publishedArtifacts = Seq(
  publishLocal in core,
  publishLocal in cli
)

lazy val `scalafix-sbt` = project
  .configs(IntegrationTest)
  .settings(
    allSettings,
    publishSettings,
    buildInfoSettings,
    Defaults.itSettings,
    ScriptedPlugin.scriptedSettings,
    libraryDependencies += "org.scalameta" % "sbt-scalahost" % scalametaV,
    sbtPlugin := true,
    testQuick := {}, // these test are slow.
    test in IntegrationTest := {
      RunSbtCommand(
        s"; plz $scala212 publishLocal " +
          "; very scalafix-sbt/scripted"
      )(state.value)
    },
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

lazy val tests = project
  .configure(setId)
  .configs(IntegrationTest)
  .settings(
    allSettings,
    noPublish,
    Defaults.itSettings,
    libraryDependencies += scalatest % IntegrationTest,
    test.in(IntegrationTest) := RunSbtCommand(
      s"; plz $scala212 publishLocal " +
        s"; such scalafix-sbt/publishLocal " +
        "; tests/it:testQuick" // hack to workaround cyclic dependencies in test.
    )(state.value),
    parallelExecution in Test := true,
    libraryDependencies ++= Seq(
      scalahost % Test,
      // integration property tests
      "org.typelevel"      %% "catalysts-platform" % "0.0.5"    % Test,
      "com.typesafe.slick" %% "slick"              % "3.2.0-M2" % Test,
      "com.chuusai"        %% "shapeless"          % "2.3.2"    % Test,
      "org.scalacheck"     %% "scalacheck"         % "1.13.4"   % Test
    )
  )
  .dependsOn(
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
    test := run.in(Compile).toTask(" --validate-links").value,
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-eval" % "6.42.0"
    )
  )
  .dependsOn(core, cli)

// Injects necessary paths into system properties to build a scalac global in tests.
def exposePaths(projectName: String,
                config: Configuration): Seq[Def.Setting[_]] = {
  def uncapitalize(s: String) =
    if (s.length == 0) ""
    else {
      val chars = s.toCharArray; chars(0) = chars(0).toLower; new String(chars)
    }
  val prefix = "sbt.paths." + projectName + "." + uncapitalize(config.name) + "."
  Seq(
    sourceDirectory in config := {
      val defaultValue = (sourceDirectory in config).value
      System.setProperty(prefix + "sources", defaultValue.getAbsolutePath)
      defaultValue
    },
    resourceDirectory in config := {
      val defaultValue = (resourceDirectory in config).value
      System.setProperty(prefix + "resources", defaultValue.getAbsolutePath)
      defaultValue
    },
    fullClasspath in config := {
      val defaultValue = (fullClasspath in config).value
      val classpath = defaultValue.files.map(_.getAbsolutePath)
      val scalaLibrary =
        classpath.map(_.toString).find(_.contains("scala-library")).get
      System.setProperty("sbt.paths.scalalibrary.classes", scalaLibrary)
      System.setProperty(prefix + "classes",
                         classpath.mkString(java.io.File.pathSeparator))
      defaultValue
    }
  )
}

lazy val isFullCrossVersion = Seq(
  crossVersion := CrossVersion.full
)

lazy val scala210 = "2.10.6"
lazy val scala211 = "2.11.11"
lazy val scala212 = "2.12.2"
lazy val ciScalaVersion = sys.env.get("CI_SCALA_VERSION")
def CiCommand(name: String)(commands: List[String]): Command =
  Command.command(name) { initState =>
    commands.foldLeft(initState) {
      case (state, command) => ci(command) :: state
    }
  }
def ci(command: String) = s"plz ${ciScalaVersion.get} $command"

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
  project.copy(base = file(newId)).settings(moduleName := newId)
}

def customScalafixVersion = sys.props.get("scalafix.version")
