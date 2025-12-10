---
id: cross-building
title: Cross-building setups
---

Cross-building the same codebase with Scala 2.13 and Scala 3.3+ is common, but
it collides with how Scalafix resolves rules and compiler options. A rule or
build-tool flag that only works on Scala 2 can crash Scalafix when the Scala 3
compilation unit is processed, and vice versa. Split your configuration per
Scala version and let the build tool wire the right file.

## Why separate configs are required today

* Scala 2 only scalac flags such as `-Ywarn-unused-import` or SemanticDB options
  using `-P:semanticdb` belong in the build tool. Those settings differ per
  target and cannot be inferred by Scalafix.
* Some built-in or community rules rely on compiler symbols present only in a
  single Scala major version.
* The CLI currently ingests a single `.scalafix.conf`; there is no per-target
  override like sbt’s `CrossVersion`.

## Limitations

Scalafix cannot conditionally toggle rules or scalac options based on the input
Scala version. Provide a separate config file per Scala version and have sbt,
mill, or your CLI invocation choose the right file along with the correct
compiler flags. See [issue #1747](https://github.com/scalacenter/scalafix/issues/1747)
for additional background.

## File layout suggestion

Keep shared defaults in one file and let version-specific files `include` it via
the HOCON `include` syntax. One convenient layout is:

```
.
└── scalafix/
    ├── common.conf
    ├── scala2.conf
    └── scala3.conf
```

`common.conf`
```scala
rules = [
  DisableSyntax,
  OrganizeImports
]

DisableSyntax {
  noFinalize = true
}
```

`scala2.conf`
```scala
include "common.conf"

rules += RemoveUnused

// Scala 2 only rule settings
RemoveUnused {
  imports = true
}
```

`scala3.conf`
```scala
include "common.conf"

rules += LeakingImplicitClassVal

// Scala 3 specific tweaks go here
OrganizeImports {
  groupedImports = Keep
}
```

### Multiple include files

You may split out even more granular snippets (for example `linting.conf`,
`rewrites.conf`) and include them from both `scala2.conf` and `scala3.conf`. The
HOCON syntax supports nested includes, so feel free to create the hierarchy that
matches your team conventions.

## Selecting the right config in sbt

Point `scalafixConfig` at the version-specific file, typically inside a helper
setting applied to all cross-built projects:

```scala
import scalafix.sbt.ScalafixPlugin.autoImport._

lazy val commonSettings = Seq(
  scalafixConfig := {
    val base = (ThisBuild / baseDirectory).value / "scalafix"
    val file =
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => base / "scala3.conf"
        case _            => base / "scala2.conf"
      }
    Some(file)
  }
)

lazy val core = project
  .settings(commonSettings)
  .settings(
    scalaVersion := "3.3.3",
    crossScalaVersions := Seq("2.13.14", "3.3.3")
  )
```

For builds that already differentiate per configuration (`Compile`, `Test`,
`IntegrationTest`), you can set `Compile / scalafixConfig` and
`Test / scalafixConfig` separately if the inputs diverge.

## Command-line usage

When invoking the CLI directly, pass the desired config with `--config`:

```
scalafix --config scalafix/scala2.conf
scalafix --config scalafix/scala3.conf
```

Your CI job can loop over each target Scala version, selecting the matching
config before running `scalafix --check`.

## Recommendations

* Keep rules that truly work on both versions inside `common.conf`.
* Manage scalac and SemanticDB flags inside the build tool per target; Scalafix
  configs focus solely on rules and rule-specific settings.
* Document in `README.md` or `CONTRIBUTING.md` which rules run on which Scala
  version to reduce confusion for new contributors.

