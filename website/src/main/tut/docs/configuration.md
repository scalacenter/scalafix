---
layout: docs
title: Configuration
---

# Configuration

Scalafix reads configuration from a file using
[HOCON](https://github.com/typesafehub/config) syntax.
The convention is to keep a file `.scalafix.conf` into the root directory of your project.
Configuration is not needed or is optional for most rules, so you may not need to create a `.scalafix.conf`.

## Rules
You can either run a default rule that comes with Scalafix or a custom rule that you write yourself.

### Default rules
The following rules come built into Scalafix.

{% assign rules = site.data.menu.options | where: "title","Rules" %}
```scala
rules = [
{% for rule in rules[0].nested_options %}  {{ rule.title }}
{% endfor %}]
```

### class:
If a scalafix rule is already on the classpath, you can classload it with the `scala:` protocol.

```scala
rule = "class:scalafix.internal.rule.ProcedureSyntax"
```

### file:
If a rule is written in a single file on local disk, you can load it with the `file:` protocol.

```scala
rule = "file:readme/MyRule.scala" // from local file
```

### http:
If a rule is written in a single source file on the internet, you can load it with the `https:`
or `http:` protocol

```scala
rule = "https://gist.githubusercontent.com/olafurpg/fc6f43a695ac996bd02000f45ed02e63/raw/f5fe47495c9b6e3ce0960b766ffa75be6d6768b2/DummyRule.scala"
```

### github:
If a rule is written in a single file and you use GitHub, you can use the `github:` protocol for
sharing your rule

```scala
rule = "github:typelevel/cats/v1.0.0"
// expands into "https://raw.githubusercontent.com/typelevel/cats/master/scalafix/rules/src/main/scala/fix/Cats_v1_0_0.scala"
```

### replace:

#### ️⚠️ Experimental

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

## lint
Override the default severity level of a {% vocabulary_ref LintMessage %} with `lint`

```scala
// Assuming 'Foo' is a rule and 'warningID'/'errorID' are LintCategory IDs.
lint.error = [ Foo.warningID ] // promote Foo.warnigID to an error
lint.warning = [ Foo.errorID ] // demote Foo.errorID to a warning
lint.info = [ Foo.errorID ] // demote Foo.errorID to info
lint.ignore = [ Foo.errorID ] // don't report Foo.errorID
lint.explain = true // print out detailed explanation for lint messages.
```

## patches
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

For more advanced use-cases, see {% doc_ref Creating your own rule %}.


