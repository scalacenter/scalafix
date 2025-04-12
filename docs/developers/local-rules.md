---
id: local-rules
title: Local rules
---

If your custom rules only target a single codebase or repository, you can
define and develop these rules as part of the same sbt build. This way, you do
not have to version your rules and publish them to a Maven or Ivy repository.

When implementing rules that are very specific to your domain, this strategy
can be interesting as it allows rules to be added or extended in the same
change sets as the domain sources they will be run against.

## Prerequisite

Make sure the sbt plugin, as well as the Scala compiler plugin and options [are
set up correctly](../users/installation.md#sbt).

> Note that any potential external Scalafix rule, loaded with the
> `scalafixDependencies` setting key, must be built and published against the
> same Scala binary version.

## Single-project sbt build

If you do not have any sub-project in your build, custom rules should be
defined under the Scalafix Ivy configuration sources directory, which by
default is `src/scalafix/scala`.

```diff
 repository
 ├── build.sbt
 ├── project
 │   └── plugins.sbt
 └── src
     ├── main
     │   └── ...
     ├── test
     │   └── ...
+    └── scalafix
+       ├── resources/META-INF/services
+       │   └── scalafix.v1.Rule // not mandatory, but allows syntax
+       |                        // `MyRule1` over `class:fix.MyRule1`
+       └── scala/fix
+           ├── MyRule1.scala
+           ├── MyRule2.scala
+           └── ...
```

```scala
// build.sbt
import _root_.scalafix.sbt.{BuildInfo => ScalafixBuildInfo}

libraryDependencies +=
  ("ch.epfl.scala" %% "scalafix-core" % ScalafixBuildInfo.scalafixVersion)
    .cross(CrossVersion.for3Use2_13) % ScalafixConfig

// Since sbt 1.10.x (https://github.com/sbt/sbt/pull/7480), scala3-library is
// not automatically added to non-standard configurations, but is needed by the
// Scala 3 compiler, so it must be added explicitly if no dependency brings it
// implicitly, which is the case here because the only dependency is for3Use2_13
libraryDependencies ++= {
  if (scalaBinaryVersion.value == "3")
    Seq("org.scala-lang" %% "scala3-library" % scalaVersion.value % ScalafixConfig)
  else
    Nil
}
```

```bash
$ sbt
> scalafix MyRule1 MyRule2
```

## Multi-project sbt build

If your build contains several sub-projects, rules can be defined either:

1. within [one of them](#within-a-given-sub-project), which is simpler but
   make them impossible to test and complicate sharing rules across
   sub-projects;
1. under [their own sub-project(s)](#as-a-separate-sub-project), allowing
   usage of [scalafix-testkit](setup.md).

### Within a given sub-project

In a large build with different code owners across sub-projects, it may
be useful to define rules within a given sub-project, without impacting the
global build definition. This helps to iterate on them before potentially
promoting them to the [build-level](#as-a-separate-sub-project).

```diff
 repository
 ├── build.sbt
 ├── project
 │   └── plugins.sbt
 └── service1
 │   └── src
 │       ├── main
 │       │   └── ...
 │       ├── test
 │       │   └── ...
+│       └── scalafix
+│           ├── resources/META-INF/services
+│           │   └── scalafix.v1.Rule // not mandatory, but allows syntax
+|           |                        // `MyRule1` over `class:fix.MyRule1`
+│           └── scala/fix
+│               ├── MyRule1.scala
+│               ├── MyRule2.scala
+│               └── ...
 ├── service2
 │   └── ...
 └── service3
     └── ...
```

```scala
// build.sbt
import _root_.scalafix.sbt.{BuildInfo => ScalafixBuildInfo}

lazy val service1 = project
  .settings(
    libraryDependencies +=
      ("ch.epfl.scala" %% "scalafix-core" % ScalafixBuildInfo.scalafixVersion)
        .cross(CrossVersion.for3Use2_13) % ScalafixConfig,
    // Since sbt 1.10.x (https://github.com/sbt/sbt/pull/7480), scala3-library is
    // not automatically added to non-standard configurations, but is needed by the
    // Scala 3 compiler, so it must be added explicitly if no dependency brings it
    // implicitly, which is the case here because the only dependency is for3Use2_13
    libraryDependencies ++= {
      if (scalaBinaryVersion.value == "3")
        Seq("org.scala-lang" %% "scala3-library" % scalaVersion.value % ScalafixConfig)
      else
        Nil
    }
  )
```

```bash
$ sbt
> service1 / scalafix MyRule1 MyRule2
```

### As a separate sub-project

Declaring your rules in a separate sub-project allows you to use
[scalafix-testkit](tutorial.md#write-unit-tests) to easily unit-test your
semantic rules.

```diff
 repository
 ├── build.sbt
 ├── project
 │   └── plugins.sbt
+├── scalafix // same source structure as the scalafix.g8 project template
+│   ├── input/src/main/scala/fix
+│   │   ├── Test1.scala
+│   │   ├── Test2.scala
+│   │   └── ...
+│   ├── output/src/main/scala/fix
+│   │   ├── Test1.scala
+│   │   ├── Test2.scala
+│   │   └── ...
+│   ├── rules/src
+│   │   └── main
+│   │       ├── resources/META-INF/services
+│   |       │   └── scalafix.v1.Rule // not mandatory, but allows syntax
+|   |       |                        // `MyRule1` over `class:fix.MyRule1`
+│   │       └── scala/fix
+│   │           ├── MyRule1.scala
+│   │           ├── MyRule2.scala
+│   │           └── ...
+│   └── tests/src/test/scala/fix
+│       └── RuleSuite.scala
 ├── service1
 │   └── ...
 ├── service2
 │   └── ...
 └── service3
     └── ...
```

```scala
// build.sbt
import _root_.scalafix.sbt.{BuildInfo => ScalafixBuildInfo}

lazy val service1 = project // sub-project where rule(s) will be used
  .dependsOn(`scalafix-rules` % ScalafixConfig)

lazy val `scalafix-rules` = (project in file("scalafix/rules"))
  .disablePlugins(ScalafixPlugin)
  .settings(
    libraryDependencies +=
      ("ch.epfl.scala" %% "scalafix-core" % ScalafixBuildInfo.scalafixVersion)
        .cross(CrossVersion.for3Use2_13)
  )

// Projects below are optional, demonstrating usage of the testkit for unit tests

lazy val `scalafix-input` = (project in file("scalafix/input"))
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-output` = (project in file("scalafix/output"))
  .disablePlugins(ScalafixPlugin)

lazy val `scalafix-tests` = (project in file("scalafix/tests"))
  .settings(
    scalafixTestkitOutputSourceDirectories :=
      (`scalafix-output` / Compile / sourceDirectories).value,
    scalafixTestkitInputSourceDirectories :=
      (`scalafix-input` / Compile / sourceDirectories).value,
    scalafixTestkitInputClasspath :=
      (`scalafix-input` / Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions :=
      (`scalafix-input` / Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      (`scalafix-input` / Compile / scalaVersion).value
  )
  .dependsOn(`scalafix-input`, `scalafix-rules`)
  .enablePlugins(ScalafixTestkitPlugin)
```

```bash
$ sbt
> scalafixTests / test
> service1 / scalafix MyRule1 MyRule2
```
