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
The sbt-plugin is the recommended integration to run semantic rules like RemoveUnusedImports or ExplicitResultTypes.

```scala
// ===> project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "{{ site.stableVersion }}")

// ===> sbt shell
> scalafixEnable // Setup scalafix for active session.
                 // Not needed if build.sbt is configured like below.
> scalafix                               // Run all rules configured in .scalafix.conf
> scalafix RemoveUnusedImports           // Run only RemoveUnusedImports rule
> myProject/scalafix RemoveUnusedImports // Run rule in one project only
> test:scalafix RemoveUnusedImports      // Run rule in single configuration
> scalafix ExplicitR<TAB>                // use tab completion
> scalafix replace:com.foobar/com.buzbaz // refactor (experimental)
> scalafix file:rules/MyRule.scala       // run local custom rule
> scalafix github:org/repo/v1            // run library migration rule

// (optional, to avoid need for scalafixEnable) permanently enable scalafix in build.sbt
// ===> build.sbt
scalaVersion := "{{ site.scala212 }}" // {{ site.scala211 }} is also supported.

// If you get "-Yrangepos is required" error or "Missing compiler plugin semanticdb",
// This setting must appear after scalacOptions and libraryDependencies.
scalafixSettings

// To configure for custom configurations like IntegrationTest
scalafixConfigure(Compile, Test, IntegrationTest)
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
sbt "scalafix RemoveUnusedImports"
git diff // should produce a diff
```

### Settings and tasks

| Name | Type | Description
|------|------|-------------
| `scalafix <rule>..` | `Unit` | Run scalafix on project sources. See {% doc_ref Rules %} or use tab completion to explore supported rules.
| `sbtfix <rule>..` | `Unit` | Run scalafix on the build sources, `*.sbt` and `project/*`. __Note__: Requires semanticdb-sbt enabled globally for semantic rules.
| `scalafixSourceRoot` | `File` | The root directory of this project.
| `scalafixScalacOptions` | `Seq[String]` | Necessary Scala compiler settings for scalafix to work.
| `scalafixVersion` | `String` | Which version of scalafix-cli to run.
| `scalafixScalaVersion` | `String` | Which Scala version of scalafix-cli to run.
| `scalafixSemanticdbVersion` | `String` | Which version of org.scalameta:semanticdb-scalac to run.
| `scalafixVerbose` | `Boolean` | If `true`, print out debug information.

### semanticdb-sbt
**⚠️ Experimental**

semanticdb-sbt is a Scala 2.10 compiler plugin that extracts semantic
information from the sbt compiler. To enable semanticdb-sbt,

```scala
// project/plugins.sbt
addCompilerPlugin("org.scalameta" % "semanticdb-sbt" % "{{ site.semanticdbSbtVersion }}" cross CrossVersion.full)
```

Once enabled, you can run scalafix rules against `project/*.scala` and `*.sbt`
files with

```scala
> reload // rebuild semanticdb for *.sbt and project/*.scala sources
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
## scalafix with scala-maven-plugin based project

There is some gotcha you will have to take care of when using scalafix-cli with a maven based project.

### semanticdb installation

Nothing is automatic here, so you will have to download `semanticdb` which correspond to you *exact* scala version, down to minor. 
For example, for scala `2.12.3`:

```
coursier fetch --intransitive org.scalameta:semanticdb-scalac_2.12.3:2.0.1
```

You can also use `wget` or a simalar tool to retrieve the jar from `https://repo1.maven.org/maven2/org/scalameta/semanticdb-scalac_2.12.3/2.0.1/semanticdb-scalac_2.12.3-2.0.1-javadoc.jar`. 

### Compile your project with semanticdb support

Let say the `semanticdb-scalac_2.12.3-2.0.1.jar` is available in `PLUGINS/semanticdb-scalac_2.12.3-2.0.1.jar` path on your file system. 

You need to recompile your project with some more flags, as follow: 

```
mvn clean test -DskipTests=true -DaddScalacArgs="-Yrangepos|-Xplugin-require:semanticdb|-Xplugin:PLUGIN/semanticdb-scalac_2.12.3-2.0.1.jar|-Ywarn-unused-import"
```

Here, we compile both main sources and tests to have semantic information generated for all of them, but we skip test execution because it is not the point of that compilation. 
The added flags for scala are given with the `addScalaArgs` option (see http://davidb.github.io/scala-maven-plugin/help-mojo.html#addScalacArgs):

- `-Yrangepos` is mandatory for `semanticdb` information gathering, 
- `-Xplugin-require:semanticdb` tells scalac to fails if it can't load the `semanticdb` plugin, 
- `-Xplugin:PLUGIN/semanticdb-scalac_2.12.3-2.0.1.jar` give the path where the `semanticdb` jar can be found, 
- `-Ywarn-unused-import` is not mandatory, but needed for rule `RemoveUnusedImports` below. 

After compilation, you must have a `target/classes/META-INF/semanticdb/`

*Important note*: you will need to recompile to get up to date `semanticdb` information after each modification. 

### Run scalafix-cli

Install and use `scala-fix` as explained above. One important note is that you need to give `--sourceroot` with the root path the same as the path where you ran your `mvn clent test` command. This may be confusing when you are on a multi-module project. 
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

``
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
