---
id: installation
title: Installation
---

To run scalafix on your project, you must first install the Scalafix sbt plugin
or command line interface. Currently, Scalafix does not provide any IDE
integrations.

| Scalafix Version | Scalameta(SemanticDB) Version | Scala Version           |
| ---------------- | ----------------------------- | ----------------------- |
| 0.5.10           | 2.1.7                         | 2.11.12 / 2.12.4        |
| @VERSION@        | @SCALAMETA@                   | @SCALA211@ / @SCALA212@ |

## sbt

The quickest way to get started is to run a **syntactic** rule like
`ProcedureSyntax`

```scala
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "@VERSION@")
```

From the sbt shell, invoke the `scalafix` task

```
> myproject/scalafix ProcedureSyntax      // run rule on main sources
> myproject/test:scalafix ProcedureSyntax // run rule on test sources
```

To run **semantic** rules like `RemoveUnusedImports` you need additional build
configuration.

```scala
// build.sbt
lazy val myproject = project.settings(
  scalaVersion := "@SCALA212@", // or @SCALA211@
  addCompilerPlugin(scalafixSemanticdb),
  scalacOptions ++= List(
    "-Yrangepos",          // required by semanticdb-scalac compiler plugin
    "-Ywarn-unused-import" // required by `RemoveUnusedImports` rule
  )
)
```

Run semantic rules from the sbt shell like normal

```
> myproject/scalafix RemoveUnusedImports
```

Beware that the `semanticdb-scalac` compiler plugin adds around 7-25% overhead
to compilation. It's recommended to provide generous JVM memory and stack
settings in `.jvmopts`:

```
-Xss8m
-Xms1G
-Xmx4G
```

### Verify installation

To verify the installation, check that scalacOptions and libraryDependencies
contain the values below.

```scala
> show scalacOptions
[info] * -Yrangepos
> show libraryDependencies
[info] * org.scalameta:semanticdb-scalac:@SCALA212@:plugin->default(compile)
```

### Example project

For a minimal example project using sbt-scalafix, see the
[scalacenter/scalafix-sbt-example](https://github.com/scalacenter/scalafix-sbt-example)
repository.

```scala
git clone https://github.com/scalacenter/sbt-scalafix-example
cd scalafix-sbt-example
sbt "scalafix RemoveUnusedImports"
git diff // should produce a diff
```

### Settings and tasks

| Name                    | Type           | Description                                                                                                                      |
| ----------------------- | -------------- | -------------------------------------------------------------------------------------------------------------------------------- |
| `scalafix <rule>..`     | `Unit`         | Run scalafix on project sources. See [Rules](rules/overview.md) or use tab completion to explore supported rules.                |
| `scalafixCli <args>`    | `Unit`         | Invoke scalafix command line interface directly. See [--help](#help) use tab completion to explore supported arguments           |
| `scalafixTest <rule>..` | `Unit`         | Similar to `scalafix` except does not write to files, only reports an error if the task would produce a diff.                    |
| `sbtfix <rule>..`       | `Unit`         | Run scalafix on the `*.sbt` and `project/*.scala` sources of this build. Only supports syntactic rules.                          |
| `sbtfixTest <rule>..`   | `Unit`         | Similar to `sbtfix` except does not write to files, only reports an error if the task would produce a diff.                      |
| `scalafixConfig`        | `Option[File]` | .scalafix.conf file to specify which scalafix rules should run. Defaults to `.scalafix.conf` in the root directory if it exists. |

### FAQ

- How to exclude/add files that are processed by the `scalafix` task?

```
unmanagedSources.in(Compile, scalafix) := unmanagedSources.in(Compile).value.filter(file => ...)
```

## Command line

First, install the [Coursier](https://github.com/coursier/coursier#command-line)
command-line interface.

Next, bootstrap a `scalafix` binary with Coursier

```sh
coursier bootstrap ch.epfl.scala:scalafix-cli_@SCALA212@:@VERSION@ -f --main scalafix.cli.Cli -o scalafix
./scalafix --help
```

### Help

```scala mdoc:--help

```

## Maven

It is possible to use Scalafix with scala-maven-plugin but it requires a custom
setup since there exists no official Scalafix plugin for Maven.

⚠️ Help wanted! The setup described here will not work for a few semantic rules
like `ExplicitResultTypes` that require access to the full classpath of your
application.

First, add the following snippet to your maven `pom.xml`:

```xml
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
              <artifactId>semanticdb-scalac_@SCALA212@</artifactId>
              <version>@SCALAMETA@</version>
            </compilerPlugin>
          </compilerPlugins>
          <addScalacArgs>-Yrangepos|-Ywarn-unused-import|-P:semanticdb:sourceroot:/path/to/root-directory</addScalacArgs>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

- Make sure to replace `/path/to/root-directory` with the absolute path to the
  root directory of Maven build. This directory must match the directory from
  where you invoke the `scalafix` command-line interface.
- `-Yrangepos` is required for `semanticdb` to function properly,
- (optional) `-Ywarn-unused-import` is required for the `RemoveUnusedImports`
  rule. If you don't run `RemoveUnusedImports` you can skip this flag. Consult
  the Scalafix documentation for each rule to see which flags it requires.

Next, compile your project with the `scalafix` profile

```
mvn clean test -DskipTests=true -P scalafix
```

We compile both main sources and tests to have semantic information generated
for all of them, but we skip test execution because it is not the point of that
compilation. For documentation about `addScalaArgs` see
[here](http://davidb.github.io/scala-maven-plugin/help-mojo.html#addScalacArgs).

> Note that will need to recompile to get up-to-date `semanticdb` information
> after each modification.

After compilation, double check that there exists a directory
`target/classes/META-INF/semanticdb/` containing files with the `.semanticdb`
extension.

Next, install the [Scalafix command line](#command-line). Finally, invoke the
`scalafix` command line interface from the same directory as the value of
`-P:semanticdb:sourceroot:/path/to/root-directory`

```sh
scalafix --rules RemoveUnusedImports
```

Congrats, if all went well you successfully ran a semantic rule for your Maven
project.

## Pants

Scalafix support is built into Pants and will run scalafmt after running
scalafix rewrite rules to maintain your target formatting. Usage instructions
can be found at https://www.pantsbuild.org/scala.html

## SNAPSHOT

Our CI publishes a snapshot release to Sonatype on every merge into master. Each
snapshot release has a unique version number, jars don't get overwritten. To
find the latest snapshot version number, go to
<https://oss.sonatype.org/content/repositories/snapshots/ch/epfl/scala/scalafix-core_2.12/>
and select the version number at the bottom, the one with the latest "Last
Modified". Once you have found the version number, adapting the version number

If using the sbt plugin

```scala
// project/plugins.sbt
resolvers += Resolver.sonatypeRepo("snapshots")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "@NIGHTLY_VERSION@-SNAPSHOT")
```

If using the command-line interface

```sh
coursier launch ch.epfl.scala:scalafix-cli_@SCALA212@:@NIGHTLY_VERSION@-SNAPSHOT -r sonatype:snapshots --main scalafix.cli.Cli -- --help
```
