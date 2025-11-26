---
id: cross-compilation
title: Cross compilation setups
---

Cross-building the same codebase with Scala 2.13 and Scala 3.3+ is common, but
it collides with how Scalafix resolves rules and compiler options. A rule or
`--scalac-options` flag that only works on Scala 2 can crash Scalafix when the
Scala 3 compilation unit is processed, and vice versa. Until Scalafix grows
conditional configuration, the safest way to avoid those failures is to split
your configuration per Scala version and let the build tool wire the right file.

## Why separate configs are required today

* Scala 2 only flags such as `-Ywarn-unused-import` or SemanticDB options using
  `-P:semanticdb` are rejected by Scala 3.
* Some built-in or custom rules depend on compiler symbols that only exist on
  one major version.
* The CLI currently ingests a single `.scalafix.conf`; there is no per-target
  override like sbt’s `CrossVersion`.

## File layout suggestion

Keep shared defaults in one file and let version-specific files `include` it via
the HOCON `include` syntax. One convenient layout is:

```
.
├── project/
│   └── build config…
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

// Scala 2 only compilers flags or rule settings
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

### Command-line usage

When invoking the CLI directly, pass the desired config with `--config`:

```
scalafix --config scalafix/scala2.conf --rules RemoveUnused
scalafix --config scalafix/scala3.conf --rules LeakingImplicitClassVal
```

Your CI job can loop over each target Scala version, selecting the matching
config before running `scalafix --check`.

## Recommendations and current limitations

* Keep rules that truly work on both versions inside `common.conf`.
* Isolate risky scalac options (`-Wunused`, `-Ywarn-unused-import`, `-P:semanticdb`)
  inside each version-specific file or sbt setting.
* Document in the repository README which rules run on which Scala version to
  reduce confusion for new contributors.

### Looking ahead

Issue [#1747](https://github.com/scalacenter/scalafix/issues/1747) tracks better
ergonomics for cross compilation. Potential improvements include:

* Conditional configuration blocks directly inside `.scalafix.conf` (for example
  `if scalaVersion.startsWith("3.")`).
* First-class support for including multiple files via CLI flags.
* Allowing rule selection based on the detected input Scala dialect.

Until those land, the include-based layout above is the recommended, battle-tested
approach.

