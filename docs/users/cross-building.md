---
id: cross-building
title: Cross-building setups
---

In a codebase cross-building against several binary versions of Scala, it is
advised to run Scalafix several times, for each Scala version. That allows to
cover version-specific files and to benefit from compiler diagnostics from
several Scala versions for common files.

However, since rules can be specific or only support a subset of features for a
given Scala version, a different set of rules or rule features might have to be
selected for each run, in order to avoid failing the entire run and preventing
relevant rules to run.

Since nothing in the current API allows rules to advertize their limitations
programmatically to Scalafix, this page explains how to do it manually.

## Why separate configs are required

* Some built-in or community rules rely on compiler symbols present only in a
  single Scala major version.
* Certain rule options may only be supported on one Scala version.
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
the HOCON `include` syntax. A convenient layout is:

```
.
├── .scalafix-common.conf
├── .scalafix-scala2.conf
└── .scalafix-scala3.conf
```


`.scalafix-common.conf`
```scala
rules = [
  DisableSyntax,
  OrganizeImports
]

DisableSyntax {
  noFinalize = true
}

```

`.scalafix-scala2.conf`
```scala
include ".scalafix-common.conf"

rules += RemoveUnused

RemoveUnused {
  imports = true
}

```

`.scalafix-scala3.conf`
```scala
include ".scalafix-common.conf"

rules += LeakingImplicitClassVal

OrganizeImports {
  groupedImports = Keep
}

```

### Multiple include files

You may split out even more granular snippets (for example `linting.conf`,
`rewrites.conf`) and include them from both `.scalafix-scala2.conf` and `.scalafix-scala3.conf`. The
HOCON syntax supports nested includes, so feel free to create the hierarchy that
matches your team conventions.

## Selecting the right configuration file

### sbt

Point `scalafixConfig` at the version-specific file, typically inside a helper
setting applied to all cross-built projects:

```scala
import scalafix.sbt.ScalafixPlugin.autoImport._

lazy val commonSettings = Seq(
  scalafixConfig := {
    val base = (ThisBuild / baseDirectory).value
    val file =
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _)) => base / ".scalafix-scala3.conf"
        case _            => base / ".scalafix-scala2.conf"
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
scalafix --config .scalafix-scala2.conf
scalafix --config .scalafix-scala3.conf
```

Your CI job can loop over each target Scala version, selecting the matching
config before running `scalafix --check`.
