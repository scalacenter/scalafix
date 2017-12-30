---
layout: docs
title: Configuration
---

# Configuration
{:.no_toc}

* TOC
{:toc}

## Per project

Scalafix reads configuration from a file using
[HOCON](https://github.com/typesafehub/config) syntax.
The convention is to keep a file `.scalafix.conf` into the root directory of your project.
Configuration is not needed or is optional for most rules, so you may not need to create a `.scalafix.conf`.

### rules
Configure which rule to run with `rules = [ ... ]`, for example

```scala
// Built in rules
rules = [
  // pre-installed rule from scalafix
  ExplicitResultTypes
  // custom rule on local disk
  "file:rules/MyRule.scala"
  // custom rule from library on GitHub
  "github:typelevel/cats/v1.0.0"
  // custom rule shared as a gist
  "https://git.io/vNXDG"
  // ...
]
```

Rules are referenced using URI syntax.
By default, a URI with no scheme such as `ExplicitResultTypes`
is interpreted as a built-in Scalafix rule.
See [here](rules) for the complete list of built-in Scalafix rules.
Scalafix supports loading custom rules with the following URI schemes:
`class:`, `file:`, `http:` and `github:`.

#### class:
If a scalafix rule is already on the classpath, you can classload it with the `class:` protocol.

```scala
rule = "class:scalafix.internal.rule.ProcedureSyntax"
```

#### file:
If a rule is written in a single file on local disk, you can load it with the `file:` protocol.

```scala
rule = "file:readme/MyRule.scala" // from local file on disk
```

#### http:
If a rule is written in a single source file on the internet, you can load it with the `https:`
or `http:` protocol

```scala
rule = "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/f5fe47495c9b6e3ce0960b766ffa75be6d6768b2/DummyRule.scala"
```

#### github:
If a rule is written in a single file and you use GitHub, you can use the `github:` protocol for
sharing your rule

```scala
rule = "github:typelevel/cats/v1.0.0"
// expands into "https://raw.githubusercontent.com/typelevel/cats/master/scalafix/rules/src/main/scala/fix/Cats_v1_0_0.scala"
```

#### replace:

To replace usage of one class/object/trait/def with another.
Note, does not move definitions like "Move" does in an IDE. This
only moves use-sites.

```scala
rule = "replace:com.company.App/io.company.App"
// From sbt shell: > scalafix replace:from/to
```

To rename a method

```scala
rule = "replace:com.company.App.start/init"
```

### lint
Override the default severity level of a {% glossary_ref LintMessage %} with `lint`

```scala
// Assuming 'Foo' is a rule and 'warningID'/'errorID' are LintCategory IDs.
lint.error = [ Foo.warningID ] // promote Foo.warnigID to an error
lint.warning = [ Foo.errorID ] // demote Foo.errorID to a warning
lint.info = [ Foo.errorID ] // demote Foo.errorID to info
lint.ignore = [ Foo.errorID ] // don't report Foo.errorID
lint.explain = true // print out detailed explanation for lint messages.
```

### patches
For simple use-cases, it's possible to write custom rules directly in `.scalafix.conf`.

```scala
patches.removeGlobalImports = [
  "scala.collection.mutable" // scala.meta.Importee
]
patches.addGlobalImports = [
  "scala.collection.immutable"
]
patches.replaceSymbols = [
  { from = "scala.collection.mutable.ListBuffer"
    to   = "com.geirsson.mutable.CoolBuffer" }
]
// Helper to see which symbols appear in your source files
debug.printSymbols = true
```

To build custom rules see {% doc_ref Rule Authors %}.

## Per source file

It's possible to suppress false positives linter message over source file regions using comments. This provides fine grained control over what to report

There are two alternative way to suppress linter messages with a single expression using scalafix:ok or with scalafix:off/on to toggle until the end of file.

### Enable or Disable until the end of file

```scala

// scalafix:off
foo(null)
1.asInstanceOf[String]
// scalafix:on

```

### Disable for an expression

```scala
List(1, "") // scalafix:ok
```

### Selectively disabling rules

Both technique above can selectively disable a list of rules. You can provide the list of rules to be disabled, separated by `,`.

```scala
// scalafix:off Disable.null, Disable.asInstanceOf
foo(null)
1.asInstanceOf[String]
// scalafix:on
```

```scala
List(1, "") // scalafix:ok NoInfer.any
```