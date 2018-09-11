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

From the sbt shell, let's run the rule `ProcedureSyntax`

```
> myproject/scalafix ProcedureSyntax
```

It's normal that the first invocation of `scalafix` takes a while to download
Scalafix artifacts from Maven Central.

If all went well and your project uses the deprecated "procedure syntax", you
should have a diff in your sources like this

```diff
-  def myProcedure {
+  def myProcedure: Unit = {
```

Next, if we run another rule like `RemoveUnused` then we get an error

```
> myproject/scalafix RemoveUnused
[error] (Compile / scalafix) scalafix.sbt.InvalidArgument: 2 errors
[E1] The semanticdb-scalac compiler plugin is required to run semantic
rules like RemoveUnused ...
[E2] The Scala compiler option "-Ywarn-unused" is required to use
RemoveUnused ...
```

The error message appears because `RemoveUnused` requires the project sources to
be compiled beforehand for the unused code analysis to function properly.

The first error message means the
[SemanticDB](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/guide.md)
compiler plugin is not enabled for this project. The second error says
`RemoveUnused` requires the Scala compiler option `-Ywarn-unused`. To fix both
problems, add the following settings to `build.sbt`

```diff
 // build.sbt
 lazy val myproject = project.settings(
   scalaVersion := "@SCALA212@", // or @SCALA211@
+  addCompilerPlugin(scalafixSemanticdb), // enable SemanticDB
   scalacOptions ++= List(
+    "-Yrangepos",          // required by SemanticDB compiler plugin
+    "-Ywarn-unused-import" // required by `RemoveUnused` rule
   )
 )
```

We run `RemoveUnused` again and the error is now gone

```
> myproject/scalafix RemoveUnused
[info] Compiling 15 Scala sources to ...
[info] Running scalafix on 15 Scala sources
```

If your project has unused imports, you should see a diff like this

```diff
- import scala.util.{ Success, Failure }
+ import scala.util.Success
```

See [example project](#example-project) for a repository that demonstrates
`ProcedureSyntax` and `RemoveUnused`.

Great! You are all set to use Scalafix with sbt :)

> Beware that the SemanticDB compiler plugin in combination with `-Yrangepos`
> adds around overhead to compilation time. The exact compilation overhead
> depends on the codebase being compiled and compiler options used. It's
> recommended to provide generous JVM memory and stack settings in the file
> `.jvmopts`:
>
> ```
>   -Xss8m
>   -Xms1G
>   -Xmx4G
> ```

### Settings and tasks

| Name              | Type                       | Description                                                                                                                       |
| ----------------- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `scalafix <args>` | `InputTaskKey[Unit]`       | Invoke scalafix command line interface directly. Use tab completion to explore supported arguments or consult [--help](#help)     |
| `scalafixConfig`  | `SettingKey[Option[File]]` | .scalafix.conf file to specify which scalafix rules should run. Defaults to `.scalafix.conf` in the root directory, if it exists. |

### Main and test sources

The task `myproject/scalafix` runs for **main sources** in the project
`myproject`. To run Scalafix on **test sources**, execute
`myproject/test:scalafix` instead. To run on both main and test sources, execute
`; myproject/scalafix ; myproject/test:scalafix`

### Integration tests

By default, the `scalafix` command is enabled for the `Compile` and `Test`
configurations. To enable Scalafix for other configuration like
`IntegrationTest`, add the following to your project settings

```diff
 lazy val myproject = project
   .configs(IntegrationTest)
   .settings(
     Defaults.itSettings,
+    inConfig(IntegrationTest)(scalafixConfigSettings(IntegrationTest))
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
addCommandAlias(
  "fixTest",
  "; compile:scalafix --test ; test:scalafix --test"
)
```

### Cache in CI

To avoid binary compatibility conflicts with the sbt classpath
([example issue](https://github.com/sbt/zinc/issues/546#issuecomment-393084316)),
the Scalafix plugin uses [Coursier](https://github.com/coursier/coursier/) to
fetch Scalafix artifacts from Maven Central. These artifacts are by default
cached in the directory `$HOME/.coursier/cache`. To avoid redundant downloads on
every pull request, it's recommended to configure your CI enviroment to cache
this directory. The location can be customized with the environment variable
`COURSIER_CACHE`

```sh
export COURSIER_CACHE=$HOME/.custom-cache
```

### Run custom rules

It's possible to run custom Scalafix rules that have been published to Maven
Central. To install a custom rule, add it to `scalafixDependencies`:

```scala
// at the top of build.sbt
scalafixDependencies in ThisBuild +=
  "com.geirsson" %% "example-scalafix-rule" % "1.3.0"
```

Start sbt and type `scalafix <TAB>`, once the `example-scalafix-rule` dependency
has been downloaded the rules `SemanticRule` and `SyntacticRule` should appear
as tab completion suggestions.

```sh
$ sbt
> scalafix Syn<TAB>
> scalafix SyntacticRule
```

If all went well, you should see a diff adding the comment
`// v1 SyntacticRule!` to all Scala source files.

```diff
+// v1 SyntacticRule!
```

### Add and exclude files

By default, the task `myproject/test:scalafix` runs on the files matching
`unmanagedSources.in(Test).value`. To customize this value, add the following
setting

```scala
unmanagedSources.in(Compile, scalafix) :=
  unmanagedSources.in(Compile).value.filter(file => ...)
```

### Enable SemanticDB for current shell session

Instead of permanently enabling SemanticDB in build.sbt, use the
`scalafixEnable` command to enable SemanticDB the current active sbt shell
session.

```
> scalafixEnable
...
> scalafix ExplicitResultTypes
```

The `scalafixEnable` command automatically runs
`addCompilerPlugin(scalafixSemanticdb)` and `scalacOptions += "-Yrangepos"` for
all eligible projects in the builds. The change in Scala compiler options means
the project needs to be re-built on the next `compile`.

> The `scalafixEnable` command must be re-executed after every `reload` and when
> sbt shell is exited.

### Optionally enable SemanticDB

It's possible to optionally enable the SemanticDB compiler plugin by updating
build.sbt like this:

```diff
  // build.sbt
- addCompilerPlugin(scalafixSemanticdb)
- scalacOptions += "-Yrangepos"
+ def shouldEnableSemanticdb: Boolean = ??? // fill this part
+ libraryDependencies ++= {
+   if (shouldEnableSemanticdb) List(compilerPlugin(scalafixSemanticdb))
+   else List()
+ }
+ scalacOptions ++= {
+   if (shouldEnableSemanticdb) List("-Yrangepos")
+   else List()
+ }
```

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
sbt "scalafix RemoveUnused"
git diff // should produce a diff
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

## SNAPSHOT

Our CI publishes a snapshot release to Sonatype on every merge into master. Each
snapshot release has a unique version number, jars don't get overwritten. To
find the latest snapshot version number, go to
<https://oss.sonatype.org/content/repositories/snapshots/ch/epfl/scala/scalafix-core_2.12/>
and select the largest version number (the one with the newest "Last Modified"
timestamp). Once you have found the version number, adapt the version number in
the instructions below

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
