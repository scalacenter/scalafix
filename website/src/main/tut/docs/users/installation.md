---
layout: docs
title: Installation
---

To run scalafix on your project, you must first install the Scalafix sbt plugin
or command line interface.
Currently, Scalafix does not provide any IDE integrations with IntelliJ/ENSIME.

* TOC
{:toc}

## sbt-scalafix

_Since 0.6.0_

_Later this plugin has `scalafixConfigure` method to configure scalafix for custom configuration like IntegrationTest. 
Since 0.6.0 version it now sticks to more canonical way for such configurations. 
The plugin has `scalafixConfigSettings` that can be imported to every configuration by `inConfig(_)`. 
`Compile` and `Test` configurations import them by default._

The sbt-plugin is the recommended integration to run semantic rules like RemoveUnusedImports or ExplicitResultTypes.

```scala
// ===> project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "{{ site.stableVersion }}")

// ===> sbt shell
> scalafixEnable // Setup scalafix for active session.
                 // Not needed if build.sbt is configured like below.
> compile:scalafixCli                                            // Run all rules configured in .scalafix.conf 
                                                                 // for compile configuration
> compile:scalafixCli --rule RemoveUnusedImports                 // Run only RemoveUnusedImports rule
> myProject/compile:scalafixCli --rule RemoveUnusedImports       // Run rule in one project only
> test:scalafixCli --rule RemoveUnusedImports                    // Run rule in test configuration
> compile:scalafixCli --rule ExplicitR<TAB>                      // Use tab completion
> compile:scalafixCli --rule replace:com.foobar/com.buzbaz       // Refactor (experimental)
> compile:scalafixCli --rule file:rules/MyRule.scala             // Run local custom rule
> compile:scalafixCli --rule github:org/repo/v1                  // Run library migration rule
> compile:scalafixCli --test                                     // Make sure no files needs to be fixed


// (optional, to avoid need for scalafixEnable) permanently enable scalafix in build.sbt
// ===> build.sbt
scalaVersion := "{{ site.scala212 }}" // {{ site.scala211 }} is also supported.

// If you get "-Yrangepos is required" error or "Missing compiler plugin semanticdb",
// This setting must appear after scalacOptions and libraryDependencies.
scalafixSettings
// or scalafixLibraryDependencies (Add semanticdb-scalac compiler plugin to libraryDependencies.)
// & scalacOptions bellow

// Similarly for sbt builds
sbtfixSettings

// To configure for custom configurations like IntegrationTest
inConfig(IntegrationTest)(scalafixConfigSettings)
```

### Verify installation

To verify the installation, check that scalacOptions and libraryDependecies
contain the values below.

```scala
> show scalacOptions
[info] * -Yrangepos                   // required
[info] * -Xplugin-require:semanticdb  // recommended
[info] * -P:semanticdb:sourceroot:/x  // recommended
> show libraryDependencies
[info] * org.scalameta:semanticdb-scalac:{{ site.scalametaVersion }}:plugin->default(compile)
```

### Example project

For a minimal example project using sbt-scalafix, see the [scalacenter/scalafix-sbt-example](https://github.com/scalacenter/scalafix-sbt-example) repository.

```scala
git clone https://github.com/olafurpg/scalafix-sbt-example
cd scalafix-sbt-example
sbt "compile:scalafix RemoveUnusedImports"
git diff // should produce a diff
```

### Settings and tasks

| Name                        | Type          | Description                                                                                                                               |
| --------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------|
| `scalafixCli <args>`         | `Unit`        | Invoke scalafix command line interface directly. See {% doc_ref Installation, cli %} or use tab completion to explore supported arguments |
| `scalafix <rule>..`         | `Unit`        | Run scalafix on project sources. See {% doc_ref Rules %} or use tab completion to explore supported rules.                                |
| `scalafixTest <rule>..`     | `Unit`        | Similar to the above task with the --test parameter                                                                                       |
| `sbtfix <rule>..`           | `Unit`        | Run scalafix on the build sources, `*.sbt` and `project/*`. **Note**: Only supports syntactic rules.                                      |
| `sbtfixTest <rule>..`       | `Unit`        | Similar to the above task with the --test parameter                                                                                       |
| `scalafixConfig`            | `Option[File]`| .scalafix.conf file to specify which scalafix rules should run.                                                                           |
| `scalafixScalacOptions`     | `Seq[String]` | Necessary Scala compiler settings for scalafix to work.                                                                                   |
| `scalafixScalaVersion`      | `String`      | Which Scala version of scalafix-cli to run.                                                                                               |
| `scalafixSemanticdbVersion` | `String`      | Which version of org.scalameta:semanticdb-scalac to run.                                                                                  |
| `scalafixSourceRoot`        | `File`        | The root directory of this project.                                                                                                       |
| `scalafixVerbose`           | `Boolean`     | If `true`, print out debug information.                                                                                                   |
| `scalafixVersion`           | `String`      | Which version of scalafix-cli to run.                                                                                                     |
| `scalafixEnabled`           | `Boolean`     | Deprecated. Use the scalafixEnable command or manually configure scalacOptions/libraryDependecies/scalaVersion                            |
| `scalametaSourceroot`       | `File`        | Deprecated. Renamed to `scalafixSourceroot`                                                                                               |

### semanticdb-sbt

**⚠️ Experimental**

semanticdb-sbt is a Scala 2.10 compiler plugin that extracts semantic
information from the sbt compiler.
Note. semanticdb-sbt only works for `*.scala` files, it does not
support `*.sbt` files.

```scala
// project/plugins.sbt
addCompilerPlugin("org.scalameta" % "semanticdb-sbt" % "{{ site.semanticdbSbtVersion }}" cross CrossVersion.full)
```

Once enabled, you can run scalafix rules against `project/*.scala`:

```scala
> reload // rebuild semanticdb for project/*.scala sources
> sbtfix Sbt1
```

## scalafix-cli

The recommended way to install the scalafix command-line interface is with
[coursier](https://github.com/coursier/coursier#command-line).

### Coursier

```sh
// coursier
coursier bootstrap ch.epfl.scala:scalafix-cli_{{ site.scala212 }}:{{ site.stableVersion }} -f --main scalafix.cli.Cli -o scalafix
./scalafix --help
```

### Homebrew

```sh
// homebrew
brew install --HEAD olafurpg/scalafmt/scalafix
scalafix --help
```

### wget

```sh
// wget
wget -O scalafix https://github.com/scalacenter/scalafix/blob/master/scalafix?raw=true
./scalafix --help
```

Once the scalafix cli is installed, consult the --help page for further usage instructions.

### --help

```tut:evaluated:plain
println(scalafix.cli.Cli.helpMessage)
```

## Maven

It is possible to use scalafix with scala-maven-plugin but it requires a custom setup since there is no scalafix specific Maven plugin.

### Install semanticdb compiler plugin

First, download the `semanticdb-scalac` compiler plugin which corresponds to your exact scala version of your project, down to the patch number.

To begin with, it's recommended to install the coursier command line interface https://github.com/coursier/coursier.

```
wget https://github.com/coursier/coursier/raw/master/coursier && chmod +x coursier && ./coursier --help
```

Coursier is a tool to download and launch library artifacts.
Once you have coursier installed, assuming you are on {{ site.scala212 }}:

```
coursier fetch --intransitive org.scalameta:semanticdb-scalac_{{ site.scala212 }}:{{ site.scalametaVersion }}
```

You can also use `wget` or a simalar tool to retrieve the jar from `https://repo1.maven.org/maven2/org/scalameta/semanticdb-scalac_{{ site.scala212 }}/{{ site.scalametaVersion }}/semanticdb-scalac_{{ site.scala212 }}-{{ site.scalametaVersion }}-javadoc.jar`.

### Compile sources with semanticdb

Let's say the `semanticdb-scalac_{{ site.scala212 }}-{{ site.scalametaVersion }}.jar` is available in `PLUGINS/semanticdb-scalac_{{ site.scala212 }}-{{ site.scalametaVersion }}.jar` path on your file system.

Recompile your project using `-DaddScalacArgs` as follow:

```
mvn clean test -DskipTests=true -DaddScalacArgs="-Yrangepos|-Xplugin-require:semanticdb|-Xplugin:PLUGIN/semanticdb-scalac_{{ site.scala212 }}-{{ site.scalametaVersion }}.jar|-Ywarn-unused-import"
```

Here, we compile both main sources and tests to have semantic information generated for all of them, but we skip test execution because it is not the point of that compilation.
The added flags for scala are given with the `addScalaArgs` option (see http://davidb.github.io/scala-maven-plugin/help-mojo.html#addScalacArgs):

* `-Yrangepos` is required for `semanticdb` to function properly,
* `-Xplugin:PLUGIN/semanticdb-scalac_{{ site.scala212 }}-{{ site.scalametaVersion }}.jar` give the path where the `semanticdb` jar can be found,
* (optional) `-Xplugin-require:semanticdb` tells scalac to fails if it can't load the `semanticdb` plugin,
* (optional) `-Ywarn-unused-import` is required for the `RemoveUnusedImports` rule. If you don't run `RemoveUnusedImports` you can skip this flag. Consult the scalafix documentation for each rule to see which flags it requires.
* (optional) Customize the --sourceroot with `-P:semanticdb:sourceroot:/path/to/sourceroot` (more details below)

After compilation, double check that there exists a directory `target/classes/META-INF/semanticdb/` containing files with the `.semanticdb` extension.

_Important note_: you will need to recompile to get up-to-date `semanticdb` information after each modification.

### Run scalafix-cli

Install and use `scalafix` as explained above.
One important note is that you need to give `--sourceroot` with the root path the same as the path where you ran your `mvn clent test` command.
This may be confusing when you are on a multi-module project.
For example, if your project is:

```
somepath/scalaProject
 |- moduleA
 |    |- src/{main, test}/scala
 |    `- target
 `- moduleB
      |- src/{main, test}/scala
      `- target
```

If you compile at `scalaProject` level, you will need to invoke scalafix with:

```
scalafix --rules RemoveUnusedImports --sourceroot /path/to/somepath/scalaProject
```

But if you compiled at `moduleA` level only, you will need to use:

```
scalafix --rules RemoveUnusedImports --sourceroot /path/to/somepath/scalaProject/moduleA
```

## scalafix-core

Scalafix can be used as a library to run custom rules.

```scala
// ===> build.sbt
libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % "{{ site.stableVersion }}"
// (optional) Scala.js is also supported
libraryDependencies += "ch.epfl.scala" %%% "scalafix-core" % "{{ site.stableVersion }}"
```

Example usage of the syntactic API.

```scala
{% include MyRule.scala %}
```

```scala
println(Uppercase("object Hello { println('world) }"))
// object HELLO { PRINTLN('world) }
```

The semantic API requires a more complicated setup.
Please see {% doc_ref Rule Authors %}.

## SNAPSHOT

Our CI publishes a snapshot release to Sonatype on every merge into master.
Each snapshot release has a unique version number, jars don't get overwritten.
To find the latest snapshot version number, go to <https://oss.sonatype.org/content/repositories/snapshots/ch/epfl/scala/scalafix-core_2.12/>
and select the version number at the bottom, the one with the latest "Last Modified".
Once you have found the version number, adapting the version number

```
// If using sbt-scalafix, add to project/plugins.sbt
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "{{ site.version }}-SNAPSHOT")

// If using scalafix-cli, launch with coursier
coursier launch ch.epfl.scala:scalafix-cli_{{ site.scala212 }}:{{ site.version }}-SNAPSHOT -r sonatype:snapshots --main scalafix.cli.Cli -- --help
```
