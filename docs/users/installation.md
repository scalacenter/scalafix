---
id: installation
title: Installation
---

## Requirements

**macOS, Linux or Windows**: Scalafix runs on macOS, Linux and Windows. Every
pull request is tested on both Linux and Windows.

**Java 8 or Java 11.**

**Scala 2.12 and 2.13**

**Scala 3.x**: Scala 3 support is experimental and many built-in rules are not
supported.

## sbt

Start by installing the sbt 1.3+ plugin in `project/plugins.sbt`

```scala
// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "@VERSION@")
```

> Scalafix is no longer published for Scala 2.11. You can run the final version
> of Scalafix supporting 2.11, but all features documented below might not be
> supported.
> ```scala
> // project/plugins.sbt
> addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4") // final Scala 2.11 version
> ```

> sbt-scalafix is no longer published for sbt 0.13.x. You should be able to run
> the latest version of Scalafix with the final sbt-scalafix version published
> for sbt 0.13.x, but all features documented below might not be supported.
>
> ```scala
> // project/plugins.sbt
> addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.29") // final sbt 0.13.x version
> dependencyOverrides += "ch.epfl.scala" % "scalafix-interfaces" % "@VERSION@"
> ```

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

the Scala compiler option `-Ywarn-unused-import` (or `-Wunused:imports` in
2.13.x). To fix both problems, add the following settings to `build.sbt`

```diff
 /*
  * build.sbt
  * SemanticDB is enabled for all sub-projects via ThisBuild scope.
  * https://www.scala-sbt.org/1.x/docs/sbt-1.3-Release-Notes.html#SemanticDB+support
  */
 inThisBuild(
   List(
     scalaVersion := "@SCALA212@", // @SCALA213@, or 3.x
+    semanticdbEnabled := true, // enable SemanticDB
+    semanticdbVersion := scalafixSemanticdb.revision // only required for Scala 2.x
   )
 )

 lazy val myproject = project.settings(
   scalacOptions += "-Ywarn-unused-import" // required by `RemoveUnused` rule
 )
```

```diff
 /*
  * build.sbt
  * SemanticDB is enabled only for a sub-project.
  * https://www.scala-sbt.org/1.x/docs/sbt-1.3-Release-Notes.html#SemanticDB+support
  */
 lazy val myproject = project.settings(
   scalaVersion := "@SCALA212@", // @SCALA213@, or 3.x
+  semanticdbEnabled := true, // enable SemanticDB
+  semanticdbVersion := scalafixSemanticdb.revision, // only required for Scala 2.x
+  scalacOptions += "-Ywarn-unused-import" // Scala 2.x only, required by `RemoveUnused`
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
> 
> You can also use project scoped settings if you don't want to mix 
> SemanticDB settings with your sub-projects which don't use it, 
> rather than using ThisBuild scoped settings.

### Settings and tasks

| Name | Type | Description
| ---- | ---- | -----------
| `scalafix <args>` | `InputKey[Unit]` | Invoke scalafix command line interface directly. Use tab completion to explore supported arguments or consult [--help](#help).
| `scalafixAll <args>` | `InputKey[Unit]` | Invoke `scalafix` across all configurations where scalafix is [enabled](#integration-tests).
| `scalafixCaching` | `SettingKey[Boolean]`  | Controls whether 2 successive invocations of the `scalafix` `InputTask` with the same arguments & configuration should be incremental. Defaults to `true`. When enabled, use the `--no-cache` argument to force an exhaustive run.
| `scalafixConfig` | `SettingKey[Option[File]]` | `.scalafix.conf` file to specify which scalafix rules should run, together with their potential options. Defaults to `.scalafix.conf` in the root directory, if it exists.
| `scalafixDependencies` | `SettingKey[Seq[ModuleID]]` | Dependencies making [custom rules](#run-custom-rules) available via their simple name. Must be set in `ThisBuild`. Defaults to `Nil`.
| `scalafixOnCompile` | `SettingKey[Boolean]` | When `true`, Scalafix rule(s) declared in `scalafixConfig` are run on compilation, applying rewrites and failing on lint errors. Defaults to `false`.
| `scalafixResolvers` | `SettingKey[Seq[Repository]]` | Custom resolvers where `scalafixDependencies` are resolved from. Must be set in `ThisBuild`. Defaults to: Ivy2 local, Maven Central, Sonatype releases & Sonatype snapshots.
| `scalafixScalaBinaryVersion` | `SettingKey[String]` | Scala binary version used for Scalafix execution. Must be set in `ThisBuild`. Defaults to 2.12. For advanced rules such as ExplicitResultTypes to work, it must match the binary version defined in the build for compiling sources. Note that `scalafixDependencies` artifacts must be published against that Scala version.


### Main and test sources

The task `myproject/scalafix` runs for **main sources** in the project
`myproject`. To run Scalafix on **test sources**, execute
`myproject/test:scalafix` instead. To run on both main and test sources, execute
`myproject/scalafixAll`.

### Integration tests

By default, the `scalafix` command is enabled for the `Compile` and `Test`
configurations, and `scalafixAll` will run on both of them. To enable
Scalafix for other configuration like `IntegrationTest`, add the following
to your project settings

```diff
 lazy val myproject = project
   .configs(IntegrationTest)
   .settings(
     Defaults.itSettings,
+    scalafixConfigSettings(IntegrationTest)
     // ...
   )
```

### Multi-module builds

Both the `scalafix` & the `scalafixAll` task aggregate like the `compile`
and `test` tasks. To run Scalafix on all projects for both main and test
sources you can execute `scalafixAll`.

### Enforce in CI

To automatically enforce that Scalafix has been run on all sources, use
`scalafix --check` instead of `scalafix`. This task fails the build if running
`scalafix` would produce a diff or a linter error message is reported.

Use `scalafixAll --check`  to enforce Scalafix on your entire project.

### Cache in CI

To avoid binary compatibility conflicts with the sbt classpath
([example issue](https://github.com/sbt/zinc/issues/546#issuecomment-393084316)),
the Scalafix plugin uses [Coursier](https://github.com/coursier/coursier/) to
fetch Scalafix artifacts from Maven Central. These artifacts are by default
cached [inside the home directory](https://get-coursier.io/docs/cache.html#default-location).
To avoid redundant downloads on every pull request, it's recommended to configure
your CI enviroment to cache this directory. The location can be customized with
the environment variable `COURSIER_CACHE`

```sh
export COURSIER_CACHE=$HOME/.custom-cache
```

### Run Scalafix automatically on compile

If you set `scalafixOnCompile` to `true`, the rules declared in `.scalafix.conf`
(or in the file located by `scalafixConfig`) will run automatically each time
`compile` is invoked either explicitly or implicitly (for example when
executing `test` or `publishLocal`). Lint errors will fail that invocation,
while rewrites will be applied.

Although this looks like an easy way to use Scalafix as a linter, use this
feature with care as it has several shortcomings, for example:

1. `scalafixOnCompile := true` is dangerous on CI as a rewrite might be applied
   before a call to `scalafix[All] --check`, causing this one to run on dirty
   sources and thus pass while it should not. Make sure that `scalafixOnCompile`
   is disabled on CI or, if that is impossible, that `scalafix[All] --check`
   is the first task executed, without any other concurrently.
1. Some rules such as `RemoveUnused` can be counter-productive if applied too
   often/early, as the work-in-progress code that was just added might disappear
   after a simple `test`. 
   To make such invocations less intrusive, you can change the rules and rules
   configuration used in that case by defining in `.scalafix.conf`
   [custom values for them](configuration.md#triggered-configuration).
1. If you run many semantic rules by default, the last one(s) to run might see
   stale information and fail the invocation, which needs to be re-run manually.
   This is [not specific to `scalafixOnCompile`](https://github.com/scalacenter/scalafix/issues/1204),
   but the problem becomes much more visible with it.
1. To keep the overhead minimal, `scalafixCaching` is automatically enabled when
   `scalafixOnCompile` is, which can cause unexpected behaviors if you run into
   false positive cache hits. `scalafixCaching` can explicitly be set to
   `false` in that case.
1. Non-idempotent rewrite rules might get you in an infinite loop where sources
   never converge - not specific to `scalafixOnCompile` either, but rather
   confusing when triggered automatically.
1. Bugs in rule implementations can prevent you from getting a successful
   `compile`, blocking testing or publishing for example

### Run custom rules

It's possible to run custom Scalafix rules that have been published to Maven
Central. To install a custom rule, add it to `scalafixDependencies`
(`scalafix.sbt.ScalafixPlugin.autoImport.scalafixDependencies`):

```scala
// at the top of build.sbt
ThisBuild / scalafixDependencies +=
  "ch.epfl.scala" %% "example-scalafix-rule" % "3.0.0"
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

### Exclude files from SemanticDB (Scala 2.x only)

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

### Exclude files from Scalafix (Scala 2.x only)

By default, the `scalafix` task processes all files in a project. If you use
SemanticDB, the `scalafix` task also respects
[`-P:semanticdb:exclude`](#exclude-files-from-semanticdb-scala-2x-only).

Use `Compile / scalafix / unmanagedSources` to optionally exclude files from
the `scalafix` task.

```scala
Compile / scalafix / unmanagedSources :=
  (Compile / unmanagedSources).value
    .filterNot(file => file.getName == "Macros.scala")
```

Replace `Compile` with `Test` to customize which test sources should be
processed.

### Customize SemanticDB output directory

The `*.semanticdb` files are available in the directory referenced by the
`semanticdbTargetRoot` key, which defaults to `target/scala-x/meta`.

You can override this default to emit `*.semanticdb` files in a custom
location. For example:

```scala
semanticdbTargetRoot := target.value / "semanticdb"
```

Alternatively, you can set the `semanticdbIncludeInJar` key to request
the compiler to emit these files into the `classDirectory` so that they
are available in packaged JARs.

```scala
semanticdbIncludeInJar := true
```

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
> scalafix RemoveUnused
```

The `scalafixEnable` command automatically enables semanticdb output and adds
`scalacOptions += "-Yrangepos"` for all eligible projects in the builds. The
change in Scala compiler options means the project may need to be re-built on
the next `compile`.

> The `scalafixEnable` command must be re-executed after every `reload` and when
> sbt shell is exited.

### Verify installation

To verify that the SemanticDB compiler plugin is enabled, check that the
settings `scalacOptions` and `allDependencies` contain the values below.

```sh
> show Compile / scalacOptions
...
[info] * List(..., -Yrangepos, ...)
...
> show allDependencies
...
[info] List(..., org.scalameta:semanticdb-scalac:@SCALA212@:plugin->default(compile), ...)
...
```

### Example project

For a minimal example project using sbt-scalafix, see the
[scalacenter/sbt-scalafix-example](https://github.com/scalacenter/sbt-scalafix-example)
repository.

```sh
git clone https://github.com/scalacenter/sbt-scalafix-example
cd sbt-scalafix-example
sbt "scalafix RemoveUnused"
git diff // should produce a diff
```

## Command line

First, install the [Coursier](https://get-coursier.io/docs/cli-installation)
command-line interface.

Next, install a `scalafix` binary with Coursier

```sh
cs install scalafix
./scalafix --version # Should say @VERSION@
```

### Help

```scala mdoc:--help

```

## Plugins for other build tools

Scalafix is supported in other build tools via externally maintained plugins:

- Mill: [mill-scalafix](https://github.com/joan38/mill-scalafix)
- Gradle: [gradle-scalafix](https://github.com/cosmicsilence/gradle-scalafix)
- Maven: [scalafix-maven-plugin](https://github.com/evis/scalafix-maven-plugin)
- [Mega-Linter](https://nvuillam.github.io/mega-linter/descriptors/scala_scalafix) (only built-in syntactic rules are supported)

## SNAPSHOT

Our CI publishes a snapshot release to Sonatype on every merge into main. Each
snapshot release has a unique version number, jars don't get overwritten.

If using the sbt plugin

```diff
 // project/plugins.sbt
 addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "@VERSION@")
+resolvers += Resolver.sonatypeRepo("snapshots")
+dependencyOverrides += "ch.epfl.scala" % "scalafix-interfaces" % "@NIGHTLY_VERSION@"
```

If using the command-line interface

```sh
cs launch ch.epfl.scala:scalafix-cli_@SCALA212@:@NIGHTLY_VERSION@ -r sonatype:snapshots --main scalafix.cli.Cli -- --help
```
