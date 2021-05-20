---
id: setup
title: Setup
---

Scalafix supports writing custom linter and rewrite rules. Custom rules have
access to a rich Scalafix public API and users can run custom rules with the
same UX as built-in rules, including tab completions in the sbt-scalafix plugin.

## sbt

The quickest way to get started in sbt is to use the `scalafix.g8` project
template:

```
cd repo-name # The project you want to implement rules for.

sbt new scalacenter/scalafix.g8 --repo="Repository Name"
cd scalafix
sbt tests/test
```

The `--repo=<repository name>` option should match the name of your GitHub
repository with dashes replaced by spaces. For
`github.com/organization/scalafix-enterprise-rules`, type
`Scalafix Enterprise Rules`.

The template generates a `scalafix/` directory with four different directories
with sources.

```scala
scalafix
├── rules/src/main
│   ├── resources/META-INF/services
│   │   └── scalafix.v1.Rule // ServiceLoader configuration to load rule
│   └── scala/fix
│       └── Rewrite.scala // Implementation of a rewrite rule
├── input/src/main/scala/test
│   └── RewriteTest.scala // Unit test for rewrite rule,
│                         // must have corresponding file in output.
│                         // no corresponding output file needed.
├── output/src/main/scala/test
│   └── RewriteTest.scala // Expected output from running rewrite
│                         // on RewriteTest.scala from input project
└── tests/src/test/scala/fix
    └── RuleSuite.scala
```

- `rules`: contains rule implementations.
- `input`: input Scala source files to be analyzed by the rule. Every Scala
  source file in this directory becomes a unit test.
- `output`: this directory is only used for rewrites, it can be ignored for
  linters. The structure of the `output` project should mirror the `input`
  project file by file. The text contents of the files should be the expected
  output after running the rule on the input sources. A mismatch from an output
  file and the result of running the rewrite rule on an `input` file becomes a
  test failure.
- `tests`: a directory containing a single test suite to configure
  `scalafix-testkit`.
  
[sbt-projectmatrix](https://github.com/sbt/sbt-projectmatrix) is used to generate
several sub-projects for each directory.
- `rules` is set up to cross-publish against all Scala binary versions for which
  [`scalafix-core`](https://mvnrepository.com/artifact/ch.epfl.scala/scalafix-core)
  is available.
- `input` and `output` are cross-built against all Scala versions on which the rule
  can be applied.
- `tests` verifies each `input` sub-project against a `rules` sub-project.

The `scalafix/` directory is a self-contained sbt build and can live in the same
directory as your existing library.

To run unit tests, execute `tests/test`.

```
$ sbt
> ~tests/test
```

### Import into IntelliJ

The project generated should import into IntelliJ like a normal project. Input
and output test files are written in plain `*.scala` files and should have full
IDE support.

### Customizing input and output projects

The input and output projects are regular sbt projects and can have custom
library dependencies. To add library dependencies to the input project, add the
following sbt settings

```diff
 lazy val input = project.settings(
+  libraryDependencies += "org" %% "name" % "version"
 )
```

The only requirement is that the input project uses a Scala compiler version
that is either supported by
[SemanticDB](https://scalameta.org/docs/semanticdb/specification.html) compiler
plugin or built-in in the compiler. Supported Scala compiler versions include:

- Scala @SCALA211@
- Scala @SCALA212@
- Scala @SCALA213@
- Scala 3.x

The output project can use any Scala compiler version.

### Running a single test

To run a single test case instead of the full test suite run the following
command: `tests/testOnly *MySuite -- -z MyRegex`. This will run only test cases
with the names matching the regular expression "MyRegex" inside test suites
matching the regular expression `*MySuite`. See the ScalaTest documentation
section:
[Selecting suites and tests](http://www.scalatest.org/user_guide/using_the_runner#selectingSuitesAndTests)
for more details about the `-z` option.

## Library API

To test custom Scalafix rules outside of sbt use the library API of
`scalafix-testkit` directly. Start by adding a dependency on the
[scalafix-testkit](https://search.maven.org/artifact/ch.epfl.scala/scalafix-testkit_@SCALA212@/@VERSION@/jar)
artifact

```
cs fetch ch.epfl.scala:scalafix-testkit_@SCALA212@:@VERSION@
```

Next, create a test suite that extends `AbstractSemanticRuleSuite`

```scala
package myproject
class MyTests
    extends scalafix.testkit.AbstractSemanticRuleSuite
    with org.scalatest.funsuite.AnyFunSuiteLike {

  runAllTests()
}
```

This setup assumes there will be a resource file `scalafix-testkit.properties`
with instructions about the full classpath of the input sources and source
directories for the input and output sources. To learn more about what values
should go into `scalafix-testkit.properties`, consult the Scaladocs.

Latest Scaladoc:
[`TestkitProperties`](https://static.javadoc.io/ch.epfl.scala/scalafix-testkit_@SCALA212@/@VERSION@/scalafix/testkit/TestkitProperties.html)
