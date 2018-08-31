---
id: installation
title: Installation
---

First, make sure you are using a supported Scala compiler version.

| Scalafix  | Scala Compiler          | Scalameta   |
| --------- | ----------------------- | ----------- |
| 0.5.10    | 2.11.12 / 2.12.4        | 2.1.7       |
| @VERSION@ | @SCALA211@ / @SCALA212@ | @SCALAMETA@ |

Next, integrate Scalafix with your build tool or use the command-line interface.

## sbt

Start by installing the sbt plugin in `project/plugins.sbt`

```scala
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "@VERSION@")
```

From the sbt shell, let's run a **syntactic** rule `ProcedureSyntax`

```
> myproject/scalafix ProcedureSyntax
```

If your project uses the deprecated "procedure syntax", you should have a diff
in your sources like this

```diff
-  def unloadDrivers {
+  def unloadDrivers: Unit = {
```

Next, if we run a **semantic** rule like `RemoveUnusedImports` then we get an
error

```
> myproject/scalafix RemoveUnusedImports
[error] SemanticDB not found: META-INF/semanticdb/src/main/scala/...
```

The error message "SemanticDB not found" means the
[SemanticDB](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md)
compiler plugin is not enabled in the build. Let's fix that by adding the
following settings to `build.sbt`

```diff
 // build.sbt
 lazy val myproject = project.settings(
   scalaVersion := "@SCALA212@", // or @SCALA211@
+  addCompilerPlugin(scalafixSemanticdb),
   scalacOptions ++= List(
+    "-Yrangepos",          // required by SemanticDB compiler plugin
+    "-Ywarn-unused-import" // required by `RemoveUnusedImports` rule
   )
 )
```

We run `RemoveUnusedImports` again and the error should be gone

```
> myproject/scalafix RemoveUnusedImports
```

If your project has unused imports, you should see a diff like this

```diff
- import scala.util.{ Success, Failure }
+ import scala.util.Success
```

Great! You are all set to use Scalafix with sbt :)

> Beware that the `semanticdb-scalac` compiler plugin adds around 7-25% overhead
> to compilation. It's recommended to provide generous JVM memory and stack
> settings in `.jvmopts`:
>
> ```
>   -Xss8m
>   -Xms1G
>   -Xmx4G
> ```

### Verify installation

To verify that the SemanticDB compiler plugin is enabled, check that the
settings `scalacOptions` and `libraryDependencies` contain the values below.

```sh
> show scalacOptions
[info] * -Yrangepos
> show libraryDependencies
[info] * org.scalameta:semanticdb-scalac:@SCALA212@:plugin->default(compile)
```

### Example project

For a minimal example project using sbt-scalafix, see the
[scalacenter/scalafix-sbt-example](https://github.com/scalacenter/scalafix-sbt-example)
repository.

```sh
git clone https://github.com/scalacenter/sbt-scalafix-example
cd scalafix-sbt-example
sbt "scalafix RemoveUnusedImports"
git diff // should produce a diff
```

### Settings and tasks

| Name              | Type                       | Description                                                                                                                       |
| ----------------- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `scalafix <args>` | `TaskKey[Unit]`            | Invoke scalafix command line interface directly. Use tab completion to explore supported arguments or consult [--help](#help)     |
| `scalafixConfig`  | `SettingKey[Option[File]]` | .scalafix.conf file to specify which scalafix rules should run. Defaults to `.scalafix.conf` in the root directory, if it exists. |

### Main and test sources

The task `myproject/scalafix` runs only in the project `myproject` for main
sources, excluding test sources. To run Scalafix on test source, execute
`myproject/test:scalafix` instead. To run on both main and test sources, execute
`all myproject/scalafix myproject/test:scalafix`

### Integration tests

By default, the `scalafix` command is only enabled for the `Compile` and `Test`
configurations. To enable Scalafix for other configuration like
`IntegrationTest`, add the following to your project settings

```scala
lazy val myproject = project
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest)),
  // ...
)
```

### Multi-module builds

The `scalafix` task aggregates like the `compile` and `test` tasks. To run
Scalafix on all projects for both main and test sources you can execute
`all scalafix test:scalafix`.

Optionally, add a command alias to your build to run Scalafix on your entire
project with the shorthand `fix`

```scala
// top of build.sbt
addCommandAlias("fix", "all compile:scalafix test:scalafix")
```

### Enforce in CI

To automatically enforce that Scalafix has been run on all sources, use
`scalafix --test` instead of `scalafix`. This task fails the build if running
`scalafix` would produce a diff or a linter error message is reported.

Optionally, add a command alias to enforce Scalafix on your entire project with
the shorthand `fixTest`

```scala
// top of build.sbt
addCommandAlias("fixTest", "; compile:scalafix --test ; test:scalafix --test")
```

### Add and exclude files

By default, the task `myproject/test:scalafix` runs on the files matching
`unmanagedSources.in(Test).value`. To customize this value, add the following
setting

```scala
unmanagedSources.in(Compile, scalafix) :=
  unmanagedSources.in(Compile).value.filter(file => ...)
```

## Command line

First, install the [Coursier](https://github.com/coursier/coursier#command-line)
command-line interface.

Next, bootstrap a `scalafix` binary with Coursier

```sh
coursier bootstrap ch.epfl.scala:scalafix-cli_@SCALA212@:@VERSION@ -f --main scalafix.cli.Cli -o scalafix
./scalafix --version # Should say @VERSION@
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
