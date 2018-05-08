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
> scalafixCli                                         // Run all rules configured in .scalafix.conf
> scalafixCli --rules RemoveUnusedImports             // Run only RemoveUnusedImports rule
> myProject/scalafixCli --rules RemoveUnusedImports    // Run rule in one project only
> test:scalafixCli --rules RemoveUnusedImports         // Run rule in single configuration
> scalafixCli --rules ExplicitR<TAB>                  // Use tab completion
> scalafixCli --rules replace:com.foobar/com.buzbaz   // Refactor (experimental)
> scalafixCli --rules file:rules/MyRule.scala         // Run local custom rule
> scalafixCli --rules github:org/repo/v1              // Run library migration rule
> scalafixCli --test                                  // Make sure no files needs to be fixed


// (optional) to avoid running scalafixEnable every time, permanently enable scalafix in build.sbt
// ===> build.sbt
scalaVersion := "{{ site.scala212 }}" // {{ site.scala211 }} is also supported.
addCompilerPlugin(scalafixSemanticdb)
scalacOptions += "-Yrangepos"
```

Notes. 
* The `semanticdb-scalac` compiler plugin is required to run semantic Scalafix rules like {% rule_ref Disable %}.
* The `semanticdb-scalac` compiler plugin is not required to run syntactic rules like {% rule_ref DisableSyntax %}.
* The `semanticdb-scalac` compiler plugin produces `*.semanticdb` files in the target `META-INF/semanticdb/` directory.
* The compilation overhead of `semanticdb-scalac` depends on the compiler settings used, but by default it is around 30% 
  over regular compilation.

### Verify installation

To verify the installation, check that scalacOptions and libraryDependecies
contain the values below.

```scala
> show scalacOptions
[info] * -Yrangepos                   // required
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

| Name                        | Type          | Description                                                                                                                               |
| --------------------------- | ------------- | ------------------------------------------------------------------------------------------------------------------------------------------|
| `scalafixCli <args>`         | `Unit`        | Invoke scalafix command line interface directly. See {% doc_ref Installation, cli %} or use tab completion to explore supported arguments |
| `scalafix <rule>..`         | `Unit`        | Run scalafix on project sources. See {% doc_ref Rules %} or use tab completion to explore supported rules.                                |
| `scalafixTest <rule>..`     | `Unit`        | Similar to the above task with the --test parameter                                                                                       |
| `scalafixConfig`            | `Option[File]`| .scalafix.conf file to specify which scalafix rules should run.                                                                           |
| `scalafixScalacOptions`     | `Seq[String]` | Necessary Scala compiler settings for scalafix to work.                                                                                   |
| `scalafixScalaVersion`      | `String`      | Which Scala version of scalafix-cli to run.                                                                                               |
| `scalafixSemanticdbVersion` | `String`      | Which version of org.scalameta:semanticdb-scalac to run.                                                                                  |
| `scalafixSourceRoot`        | `File`        | The root directory of this project.                                                                                                       |
| `scalafixVerbose`           | `Boolean`     | If `true`, print out debug information.                                                                                                   |
| `scalafixVersion`           | `String`      | Which version of scalafix-cli to run.                                                                                                     |
| `scalafixEnabled`           | `Boolean`     | Deprecated. Use the scalafixEnable command or manually configure scalacOptions/libraryDependecies/scalaVersion                            |
| `scalametaSourceroot`       | `File`        | Deprecated. Renamed to `scalafixSourceroot`                                                                                               |


### FAQ
{: #sbt-faq}

* How to exclude/add files that are processed by the `scalafix` task?

```
unmanagedSources.in(Compile, scalafix) := unmanagedSources.in(Compile).value.filter(file => ...)
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

You can either install the semanticdb compiler plugin manually or let maven resolve the dependency for you.

To install the plugin manually, first, download the `semanticdb-scalac` compiler plugin which corresponds to the exact scala version of your project(down to the patch number).

It's recommended to do it via the [coursier command line interface](https://github.com/coursier/coursier) which simplifies downloading and launching library artifacts.

To install coursier:

```
wget https://github.com/coursier/coursier/raw/master/coursier && chmod +x coursier && ./coursier --help
```

Then, once you have coursier installed, assuming you are on {{ site.scala212 }}, download the plugin:

```
coursier fetch --intransitive org.scalameta:semanticdb-scalac_{{ site.scala212 }}:2.1.7
```

Alternatively, you can also use `wget` or a simalar tool to retrieve the jar from `https://repo1.maven.org/maven2/org/scalameta/semanticdb-scalac_{{ site.scala212 }}/2.1.7/semanticdb-scalac_{{ site.scala212 }}-2.1.7.jar`.

If you prefer using maven profile instead of manual download, add the following snippet to your maven `pom.xml`:

```
<profile>
  <id>scalafix</id>
  <build>
    <plugins>
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <configuration>
          <compilerPlugins>
            <compilerPlugin>
              <groupId>org.scalameta</groupId>
              <artifactId>semanticdb-scalac_{{ site.scala212 }}</artifactId>
              <version>2.1.7</version>
            </compilerPlugin>
          </compilerPlugins>
          <addScalacArgs>-Yrangepos|-Ywarn-unused-import</addScalacArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

### Compile sources with semanticdb

If you installed the semanticdb plugin manually, you have to locate it first.

Let's say the `semanticdb-scalac_{{ site.scala212 }}-2.1.7.jar` is available in `PLUGINS/semanticdb-scalac_{{ site.scala212 }}-2.1.7.jar` path on your file system.

Then, recompile your project using `-DaddScalacArgs` as follows:

```
mvn clean test -DskipTests=true -DaddScalacArgs="-Yrangepos|-Xplugin-require:semanticdb|-Xplugin:PLUGIN/semanticdb-scalac_{{ site.scala212 }}-2.1.7.jar|-Ywarn-unused-import"
```

If you used maven profile instead, you can do the following:

```
mvn clean test -DskipTests=true -P scalafix
```

We compile both main sources and tests to have semantic information generated for all of them, but we skip test execution because it is not the point of that compilation.
The added flags for scala are given with the `addScalaArgs` option (see http://davidb.github.io/scala-maven-plugin/help-mojo.html#addScalacArgs):

* `-Yrangepos` is required for `semanticdb` to function properly,
* `-Xplugin:PLUGIN/semanticdb-scalac_{{ site.scala212 }}-2.1.7.jar` give the path where the `semanticdb` jar can be found,
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

## Pants

Scalafix support is built into Pants and will run scalafmt after running scalafix rewrite rules to maintain your target formatting.  Usage instructions can be found at https://www.pantsbuild.org/scala.html 

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
