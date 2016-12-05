import sbt.ScriptedPlugin
import sbt.ScriptedPlugin._
organization in ThisBuild := "ch.epfl.scala"
version in ThisBuild := scalafix.Versions.nightly

lazy val buildSettings = Seq(
  assemblyJarName in assembly := "scalafix.jar",
  // See core/src/main/scala/ch/epfl/scala/Versions.scala
  scalaVersion := scalafix.Versions.scala,
  updateOptions := updateOptions.value.withCachedResolution(true)
)

lazy val jvmOptions = Seq(
  "-Xss4m"
)

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

lazy val commonSettings = Seq(
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
  scalacOptions := compilerOptions,
  scalacOptions in (Compile, console) := compilerOptions :+ "-Yrepl-class-based",
  testOptions in Test += Tests.Argument("-oD")
)

lazy val publishSettings = Seq(
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq(
    "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/scalacenter/scalafix")),
  autoAPIMappings := true,
  apiURL := Some(url("https://scalacenter.github.io/scalafix/docs/")),
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
  publish := {},
  publishLocal := {}
)

lazy val allSettings = commonSettings ++ buildSettings ++ publishSettings

lazy val root = project
  .in(file("."))
  .settings(moduleName := "scalafix")
  .settings(allSettings)
  .settings(noPublish)
  .settings(
    initialCommands in console :=
      """
        |import scala.meta._
        |import scalafix._
      """.stripMargin
  )
  .aggregate(
    `scalafix-nsc`,
    `scalafix-tests`,
    core,
    cli,
    readme,
    `scalafix-sbt`
  )
  .dependsOn(core)

lazy val core = project
  .settings(allSettings)
  .settings(
    moduleName := "scalafix-core",
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    libraryDependencies ++= Seq(
      "com.lihaoyi"    %% "sourcecode"   % "0.1.2",
      "org.scalameta"  %% "scalameta"    % Build.metaV,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      // Test dependencies
      "org.scalatest"                  %% "scalatest" % Build.testV % "test",
      "com.googlecode.java-diff-utils" % "diffutils"  % "1.3.0"     % "test"
    )
  )

lazy val `scalafix-nsc` = project
  .settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalameta"  %% "scalameta"     % Build.metaV % "provided",
      "org.scalatest"  %% "scalatest"     % Build.testV % Test
    ),
    // sbt does not fetch transitive dependencies of compiler plugins.
    // to overcome this issue, all transitive dependencies are included
    // in the published compiler plugin.
    publishArtifact in Compile := true,
    assemblyJarName in assembly :=
      name.value + "_" +
        scalaVersion.value + "-" +
        version.value + "-assembly.jar",
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    Keys.`package` in Compile := {
      val slimJar = (Keys.`package` in Compile).value
      val fatJar =
        new File(crossTarget.value + "/" + (assemblyJarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      slimJar
    },
    packagedArtifact in Compile in packageBin := {
      val temp = (packagedArtifact in Compile in packageBin).value
      val (art, slimJar) = temp
      val fatJar =
        new File(crossTarget.value + "/" + (assemblyJarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      (art, slimJar)
    },
    exposePaths("scalafixNsc", Test)
  )
  .dependsOn(core)

lazy val cli = project
  .settings(allSettings)
  .settings(packSettings)
  .settings(
    moduleName := "scalafix-cli",
    packJvmOpts := Map(
      "scalafix" -> jvmOptions,
      "scalafix_ng_server" -> jvmOptions
    ),
    mainClass in assembly := Some("scalafix.cli.Cli"),
    packMain := Map(
      "scalafix" -> "scalafix.cli.Cli",
      "scalafix_ng_server" -> "com.martiansoftware.nailgun.NGServer"
    ),
    libraryDependencies ++= Seq(
      "com.github.scopt"           %% "scopt"         % "3.5.0",
      "com.github.alexarchambault" %% "case-app"      % "1.1.0-RC3",
      "com.martiansoftware"        % "nailgun-server" % "0.9.1"
    )
  )
  .dependsOn(core % "compile->compile;test->test")

lazy val publishedArtifacts = Seq(
  publishLocal in `scalafix-nsc`,
  publishLocal in core
)

lazy val `scalafix-sbt` = project.settings(
  allSettings,
  ScriptedPlugin.scriptedSettings,
  sbtPlugin := true,
  scripted := scripted.dependsOn(publishedArtifacts: _*).evaluated,
  scalaVersion := "2.10.5",
  moduleName := "sbt-scalafix",
  sources in Compile +=
    baseDirectory.value / "../core/src/main/scala/scalafix/Versions.scala",
  scriptedLaunchOpts := Seq(
    "-Dplugin.version=" + version.value,
    // .jvmopts is ignored, simulate here
    "-XX:MaxPermSize=256m",
    "-Xmx2g",
    "-Xss2m"
  ),
  scriptedBufferLog := false
)

lazy val `scalafix-tests` = project
  .settings(
    allSettings,
    noPublish,
    test in Test := (test in Test).dependsOn(publishedArtifacts: _*).value,
    parallelExecution in Test := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %% "ammonite-ops" % "0.8.0",
      "org.scalatest" %% "scalatest"    % Build.testV % "test"
    )
  )
  .dependsOn(core)

lazy val readme = scalatex
  .ScalatexReadme(projectId = "readme",
                  wd = file(""),
                  url = "https://github.com/scalacenter/scalafix/tree/master",
                  source = "Readme")
  .settings(allSettings)
  .settings(noPublish)
  .dependsOn(core)
  .dependsOn(cli)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "util-eval" % "6.34.0"
    ),
    dependencyOverrides += "com.lihaoyi" %% "scalaparse" % "0.3.1"
  )

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
