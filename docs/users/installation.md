---
id: installation
title: Installation
---

## Requirements

**macOS, Linux and Windows**: Scalafix runs on macOS, Linux and Windows. Every
pull request is tested on both Linux and Windows.

**Java 8**. Scalafix supports only Java 8 at the moment. Running Scalafix on
Java 9+ results in `error: unable to load symbol table` errors. See
[#880](https://github.com/scalacenter/scalafix/issues/880) for updates on adding
support for Java 11.

**Scala 2.11 and 2.12**: Scalafix works only with the latest version of Scala
2.11 and Scala 2.12. See
[scalameta/scalameta#1695](https://github.com/scalameta/scalameta/issues/1695)
for updates on adding support for Scala 2.13 once it's out.

| Scalafix  | Scala Compiler          | Scalameta   |
| --------- | ----------------------- | ----------- |
| @VERSION@ | @SCALA211@ / @SCALA212@ | @SCALAMETA@ |
| 0.5.10    | 2.11.12 / 2.12.4        | 2.1.7       |

## sbt

Start by installing the sbt plugin in `project/plugins.sbt`

```scala
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "@VERSION@")
```

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ch.epfl.scala/sbt-scalafix/badge.svg)](https://maven-badges.herokuapp.com/maven-central/ch.epfl.scala/sbt-scalafix)

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

The first error message means the
[SemanticDB](https://scalameta.org/docs/semanticdb/guide.html) compiler plugin
is not enabled for this project. The second error says `RemoveUnused` requires
the Scala compiler option `-Ywarn-unused`. To fix both problems, add the
following settings to `build.sbt`

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

For `project/*.scala` files, add
`import scalafix.sbt.ScalafixPlugin.autoImport._` to the top of the file to
resolve `scalafixSemanticdb`.

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
> adds overhead to compilation time. The exact compilation overhead depends on
> the codebase being compiled and compiler options used. It's recommended to
> provide generous JVM memory and stack settings in the file `.jvmopts`:
>
> ```
>   -Xss8m
>   -Xms1G
>   -Xmx8G
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
`scalafix --check` instead of `scalafix`. This task fails the build if running
`scalafix` would produce a diff or a linter error message is reported.

Optionally, add a command alias to enforce Scalafix on your entire project with
the shorthand `fixCheck`

```scala
// top of build.sbt
addCommandAlias(
  "fixCheck",
  "; compile:scalafix --check ; test:scalafix --check"
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
Central. To install a custom rule, add it to `scalafixDependencies`
(`scalafix.sbt.ScalafixPlugin.autoImport.scalafixDependencies`):

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

### Exclude files from SemanticDB

By default, the SemanticDB compiler plugin will process all files in a project.

Use `-P:semanticdb:exclude:<regex>` to exclude files from the SemanticDB
compiler plugin.

```scala
scalacOptions += "-P:semanticdb:exclude:Macros.scala"
```

Separate multiple patterns with pipe `|` to exclude multiple files.

```scala
scalacOptions += "-P:semanticdb:exclude:Macros.scala|Schema.scala"
```

To learn more about SemanticDB compiler options visit
https://scalameta.org/docs/semanticdb/guide.html#scalac-compiler-plugin

> Avoid using slashes like `/` in `-P:semanticdb:exclude` since that will not
> work on Windows. The argument is compiled to a regular expression and gets
> matched against the `java.io.File.getAbsolutePath` representation of each
> file.

### Exclude files from Scalafix

By default, the `scalafix` task processes all files in a project. If you use
SemanticDB, the `scalafix` task also respects
[`-P:semanticdb:exclude`](#exclude-files-from-semanticdb).

Use `unmanagedSources.in(Compile, scalafix)` to optionally exclude files from
the `scalafix` task.

```scala
unmanagedSources.in(Compile, scalafix) :=
  unmanagedSources.in(Compile).value
    .filterNot(file => file.getName == "Macros.scala")
```

Replace `Compile` with `Test` to customize which test sources should be
processed.

### Customize SemanticDB output directory

By default, the SemanticDB compiler plugin emits `*.semanticdb` files in the
`classDirectory`.

Use `-P:semanticdb:targetroot:path` to configure SemanticDB to emit
`*.semanticdb` files in a custom location. For example:

```scala
scalacOptions += {
  val targetroot = target.value / "semanticdb"
  s"-P:semanticdb:targetroot:$targetroot"
}
```

To learn more about SemanticDB compiler options visit
https://scalameta.org/docs/semanticdb/guide.html#scalac-compiler-plugin

### Disable Scalafix for specific project

Use `.disablePlugins(ScalafixPlugin)` to disable Scalafix for a particular
project.

```diff
  lazy val myproject = project
    .settings(...)
+   .disablePlugins(ScalafixPlugin)
```

When using Scala.js or Scala Native, use `.jsConfigure` or `.nativeConfigure` to
disable Scalafix for only the Scala.js or Scala Native project. For example:

```diff
  lazy val myproject = crossProject(JVMPlatform, JSPlatform)
    .settings(...)
+   .jsConfigure(_.disablePlugins(ScalafixPlugin))
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
