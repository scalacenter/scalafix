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
cd reponame # The project you want to implement rules for.

sbt new scalacenter/scalafix.g8 --rule="reponame" --version="v1.0"
cd scalafix
sbt tests/test
```

The `--rule=<reponame>` option should match the name of your GitHub repository.
The template generates a `scalafix/` directory with four different sbt projects

```scala
scalafix
├── rules  // rule implementations
├── input  // code to be analyzed by rule
├── output // expected output from running input on rules
└── tests  // small project where unit tests run
```

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
that is supported by
[SemanticDB](https://github.com/scalameta/scalameta/blob/master/semanticdb/semanticdb3/semanticdb3.md)
compiler plugin. Supported Scala compiler versions include:

- Scala @SCALA211@
- Scala @SCALA212@

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
coursier fetch ch.epfl.scala:scalafix-testkit_@SCALA212@:@VERSION@
```

Next, create a test suite that extends the class `SemanticRuleSuite`

```scala
package myproject
class MyTests extends scalafix.testkit.SemanticRuleSuite {
  runAllTests()
}
```

This setup assumes there will be a resource file `scalafix-testkit.properties`
with instructions about the full classpath of the input sources and source
directories for the input and output sources. To learn more about what values
should go into `scalafix-testkit.properties`, consult the Scaladocs.

Latest Scaladoc:
[`TestkitProperties`](https://static.javadoc.io/ch.epfl.scala/scalafix-testkit_@SCALA212@/@VERSION@/scalafix/testkit/TestkitProperties.html)
