---
id: tutorial
title: Tutorial
---

In this tutorial, you will learn how to

- write unit tests for rewrite and linter rules
- use pattern matching to find interesting tree nodes
- use `SymbolInformation` to look up method signatures
- use `Diagnostic` to report linter errors
- use `withConfiguration` to make a rule configurable
- publish the rule so others can try it on their own codebase

We are going to implement two different rules. The first rule is a semantic
rewrite `NamedLiteralArguments` that produces the following diff

```diff
  def complete(isSuccess: Boolean): Unit = ???
- complete(true)
+ complete(isSuccess = true)
```

The second rule is a syntactic linter `NoLiteralArguments` that reports an error
when a literal is used in argument position

```scala
test/NamedLiteralArguments.scala:9:12: error: [NoLiteralArguments]:
Use named arguments for literals such as 'parameterName = true'
  complete(true)
           ^^^^
```

Let's get started!

## Import the build

Start by cloning the repository
[olafurpg/named-literal-arguments](https://github.com/olafurpg/named-literal-arguments).

```
git clone https://github.com/olafurpg/named-literal-arguments.git
cd named-literal-arguments
cd scalafix
sbt
...
[info] sbt server started at local://$HOME/.sbt/1.0/server/93fc24de3bb97dec3e5b/sock
sbt:scalafix>
```

This starts an sbt shell session from where you can run the test suite with
`tests/test`.

Optionally, if you use IntelliJ, import the build like normal with the action
"New project from existing sources" and select the `scalafix/build.sbt` file.

The sections in this tutorial follow the chronological order of the git history
so feel free to checkout older commits.

First we implement `NamedLiteralArguments`.

## Write unit tests

The build we just cloned is composed of four sub-projects

- `rules`: where the `NamedLiteralArguments` rewrite rule is implemented
- `input`: where the code before the rewrite gets applied is written
- `output`: a mirror of the `input` project except with the expected code after
  the rewrite has applied to the input files
- `tests`: where we run the unit tests

For every file in the `input` project there should be a matching file in the
`output` project

```sh
input/src
└── main
    └── scala
        └── fix
            └── NamedLiteralArguments.scala
output/src
└── main
    └── scala
        └── fix
            └── NamedLiteralArguments.scala
```

Checkout the commit
[55f9196163ab0a](https://github.com/olafurpg/named-literal-arguments/commit/55f9196163ab0a5dde22364ab1f7880bf4a5dc54),
run `tests/test` and see the tests fail

```diff
 > ~tests/test
--- obtained
+++ expected
@@ -4,3 +4,3 @@
   def complete(isSuccess: Boolean): Unit = ()
-  complete(true)
+  complete(isSuccess = true)
 }
```

The diff tells us that the expected fix (contents of `output` file) does not
match the output from running `NamedLiteralArguments` on the `input` file. We
expected the output to be `complete(isSuccess = true)` but the obtained output
was `complete(true)`. The `NamedLiteralArguments` rule currently returns
`Patch.empty` so the test failure is expected.

## Use pattern matching to find interesting tree nodes

We update the rule implementation to traverse the syntax tree and find
occurrences of the literal `true`.

```scala
doc.tree.collect {
  case t @ q"true" => Patch.addLeft(t, "isSuccess = ")
}.asPatch
```

Let's break this down:

- `doc.tree.collect { case => ...}`: perform a top-to-bottom traversal of the
  syntax tree
- we construct a Scalameta
  ["quasiquote"](https://scalameta.org/docs/trees/quasiquotes.html) pattern
  `q"true"` which matches any tree node that represents the boolean literal
  `true`.
- `Patch.addLeft(t, "isSuccess = ")`: describes a refactoring on the source code
  that adds the string `isSuccess =` to the left side of the `true` literal.
- `List[Patch].asPatch`: helper method to convert a list of patches into a
  single patch.

This solution is simple but it is incomplete

- the rewrite triggers only for the literal `true` but not `false`
- the rewrite triggers for any `true` literal even if it is not a function
  argument. For example, `val done = true` becomes
  `val done = isSuccess = true`.

The first improvement we make is to handle both `true` and `false` literals.

```diff
-  case t @ q"true" =>
+  case t @ Lit.Boolean(_) =>
```

We replace the `q"true"` quasiquote with `Lit.Boolean(_)`. Quasiquotes are great
for constructing static tree nodes but pattern matching against named tree nodes
like `Lit.Boolean(_)` can be more flexible when you need fine-grained control.

To find the name of a tree node you can use
[AST Explorer](http://astexplorer.net/#/gist/ec56167ffafb20cbd8d68f24a37043a9/74efb238ad02abaa8fa69fc80342563efa8a1bdc)
or `tree.structure`. First, make sure you have the following imports

```scala mdoc:silent
import scalafix.v1._
import scala.meta._
```

Next, use the `.structure` and `.structure(width: Int)` extension methods on
trees.

```scala mdoc
println(q"complete(true)".structure)     // line wrap at 80th column
println(q"complete(true)".structure(30)) // line wrap at 30th column
```

The output of `tree.structure` can be copy-pasted for use in pattern matching.

The next improvement is to ensure we only rewrite boolean literals that appear
in function argument position. Previously, the rewrite would replace appearances
of `true` anywhere, producing problematic diffs like this

```diff
- val isComplete = true
+ val isComplete = isSuccess = true
```

To fix this bug, we first match function call nodes `Term.Apply` and pattern
match only `Lit.Boolean` that appear in argument position

```scala
case Term.Apply(_, args) =>
  args.collect {
    case t @ Lit.Boolean(_) =>
      Patch.addLeft(t, "isSuccess = ")
  }
```

## Use `SymbolInformation` to lookup method signatures

Our rule is still unfinished because we have hard-coded `isSuccess`. Let's add a
test case to reproduce this bug

```scala
def complete(isSuccess: Boolean): Unit = ()
def finish(isError: Boolean): Unit = ()
complete(true)
finish(true)
```

The rule currently produces `finish(isSuccess = true)` but the correct solution
is to produce `finish(isError = true)`.

To fix this bug, we start by capturing the called method into a variable `fun`

```diff
- case Term.Apply(_, args) =>
+ case Term.Apply(fun, args) =>
```

We update the call to `args.collect` to include the index of the argument

```diff
- args.collect { case Lit.Boolean(_) => ...
+ args.zipWithIndex.collect { case (Lit.Boolean(_), i) => ...
```

Next, we use the method `Tree.symbol.info` to query information about the method
we are calling

```diff
  case Lit.Boolean(_) =>
+   fun.symbol.info match {
+     case Some(info) =>
        // ...
+     case None =>
+       // Do nothing, no information about this symbol.
+       Patch.empty
+   }
```

- `Tree.symbol` returns a `Symbol`, which represents the unique identifier of
  definition such as a `val` or a `class`.
- `Symbol.info` returns a
  [`SymbolInformation`](https://static.javadoc.io/ch.epfl.scala/scalafix-core_2.12/@VERSION@/scalafix/v1/SymbolInformation.html),
  which contains metadata about that symbol.

Next, we use `SymbolInformation.signature` to see if the symbol is a method with
a non-empty parameter list.

```diff
+ info.signature match {
+   case method: MethodSignature if method.parameterLists.nonEmpty =>
      // ...
+   case _ =>
+     // Do nothing, the symbol is not a method with matching signature
+     Patch.empty
+ }
```

The final step is to extract the parameter at the index of the argument

```scala
val parameter = method.parameterLists.head(i)
val parameterName = parameter.displayName
Patch.addLeft(t, s"$parameterName = ")
```

That completes the `NamedLiteralArguments` rule! Run all tests and we see they
pass. Putting it together, the final code for the rule looks like this

```scala
// NamedLiteralArguments.scala
package fix

import scalafix.v1._
import scala.meta._

class NamedLiteralArguments
    extends SemanticRule("NamedLiteralArguments") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case Term.Apply(fun, args) =>
        args.zipWithIndex.collect {
          case (t @ Lit.Boolean(_), i) =>
            fun.symbol.info match {
              case Some(info) =>
                info.signature match {
                  case method: MethodSignature
                      if method.parameterLists.nonEmpty =>
                    val parameter = method.parameterLists.head(i)
                    val parameterName = parameter.displayName
                    Patch.addLeft(t, s"$parameterName = ")
                  case _ =>
                    // Do nothing, the symbol is not a method with matching signature
                    Patch.empty
                }
              case None =>
                // Do nothing, no information about this symbol.
                Patch.empty
            }
        }
    }.flatten.asPatch
  }
}
```

Next, we learn how to implement a syntactic linter.

## Use `Diagnostic` to report linter errors

Let's say we want to report an error message when an argument is a literal
instead of automatically inserting the parameter name. The user would see a
diagnostic like this

```scala
test/NamedLiteralArguments.scala:9:12: error: [NoLiteralArguments]:
Use named arguments for literals such as 'parameterName = true'
  complete(true)
           ^^^^
```

The benefit of making `NamedLiteralArguments` a syntactic linter instead of a
semantic rewrite is that it's simpler to run syntactic rules since they don't
require compilation. However, the downside is that we can't look up the correct
parameter name. The linter can be syntactic because it doesn't need to use
`SymbolInformation` to look up the parameter name.

First, let's create a diagnostic that produces the error message

```scala
case class LiteralArgument(literal: Lit) extends Diagnostic {
  override def position: Position = literal.pos
  override def message: String =
    s"Use named arguments for literals such as 'parameterName = $literal'"
}
```

Next, we create a new syntactic rule `NoLiteralArguments`.

```scala
class NoLiteralArguments extends SyntacticRule("NoLiteralArguments") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    // ...
  }
}
```

Next, update `META-INF/services/scalafix.v1.Rule` to include the new rule so
that Scalafix can load the rule by it's name `rules = [ NoLiteralArguments ]`.
Consult the JDK
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
documentation to learn more about how Scalafix loads rules

```diff
  fix.NamedLiteralArguments
+ fix.NoLiteralArguments
```

We create a new input file to test `NoLiteralArguments`.

```scala
/*
rule = NoLiteralArguments
 */
package test

class NoLiteralArguments {
  def complete(isSuccess: Boolean): Unit = ()
  complete(true) // assert: NoLiteralArguments
}
```

The comment `// assert: NoLiteralArguments` asserts that a diagnostic is
reported at the line of `complete(true)`. There is no need to write an output
file since linters don't modify the input source code.

Next, we write the same pattern matching logic as in `NamedLiteralArguments`

```scala
doc.tree.collect {
  case Term.Apply(_, args) =>
    args.collect {
      case t @ Lit.Boolean(_) =>
        // ....
    }
}.flatten.asPatch
```

Finally, to report a diagnostic we use `Patch.lint`

```scala
Patch.lint(LiteralArgument(t))
```

We run the tests and see that they pass. Let's add a `/*` multi-line assertion
to make sure the position and message of the diagnostic make sense

```diff
  complete(true) // assert: NoLiteralArguments
+ complete(false) /* assert: NoLiteralArguments
+          ^^^^^
+ Use named arguments for literals such as 'parameterName = false'
+ */
```

> It's a good practice to write at least one `/*` multi-line assertion for the
> position and message contents of a diagnostic.

## Use `withConfiguration` to make a rule configurable

The `NoLiteralArguments` linter reports errors for **boolean** literals but in
some cases we might want an error for other types of literals such as magic
**numbers** and **strings**. Since users may have different preferences, let's
allow them to decide which literal types to prohibit through configuration in
`.scalafix.conf`

```conf
// .scalafix.conf
NoLiteralArguments.disabledLiterals = [
  "Int",
  "String",
  "Boolean"
]
```

Let's start by adding a failing test suite by adding a new input file
`NoLiteralArgumentsConfig.scala`

```scala
/*
rule = NoLiteralArguments
NoLiteralArguments.disabledLiterals = [
  Int
  Boolean
]
 */
package test

class NoLiteralArgumentsConfig {
  def complete(message: String): Unit = ()
  def complete(count: Int): Unit = ()
  def complete(isSuccess: Boolean): Unit = ()
  complete("done") // ok, no error message
  complete(42) // assert: NoLiteralArguments
  complete(true) // assert: NoLiteralArguments
}
```

The top of the file contains `.scalafix.conf` configuration that is passed to
rules when they're loaded.

If we run `tests/test` we get an error like this

```scala
===========> Unreported <===========
test/NoLiteralArgumentsConfig.scala:16:20: error
  complete("done") // assert: NoLiteralArguments
                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
```

An "unreported" error message means we asserted a diagnostic would be reported
at this line but no diagnostic was reported. This is expected since we don't
read the configuration yet.

Start by writing a case class to hold the configuration

```scala
case class NoLiteralArgumentsConfig(
    disabledLiterals: List[String] = List("Boolean")
) {
  def isDisabled(literal: Lit): Boolean = {
    val kind = lit.productPrefix.stripPrefix("Lit.")
    disabledLiterals.contains(kind)
  }
}
```

Next we update the rule to have an instance of the configuration

```diff
- class NoLiteralArguments extends SyntacticRule("NoLiteralArguments")
+ class NoLiteralArguments(config: NoLiteralArgumentsConfig)
+     extends SyntacticRule("NoLiteralArguments")
+   def this() = this(NoLiteralArgumentsConfig())
```

> It's important to keep an empty constructor `def this() = ...` so that
> Scalafix can load the rule. If we forget the empty constructor we get an error
> like this: "java.lang.NoSuchMethodException: Provider fix.NoLiteralArguments
> could not be instantiated"

Next, we create a companion object with decoders to read `.scalafix.conf`
configuration into `NoLiteralArgumentsConfig`.

```scala
object NoLiteralArgumentsConfig {
  def default = NoLiteralArgumentsConfig()
  implicit val surface =
    metaconfig.generic.deriveSurface[NoLiteralArgumentsConfig]
  implicit val decoder =
    metaconfig.generic.deriveDecoder(default)
}
```

To learn more about decoding configuration, consult the
[metaconfig docs](https://olafurpg.github.io/metaconfig/).

Next, we override the `withConfiguration` method to read user configuration.

```diff
  class NoLiteralArguments(config: NoLiteralArgumentsConfig)
      extends SyntacticRule("NoLiteralArguments") {
+   override def withConfiguration(config: Configuration): Configured[Rule] =
+     config.conf
+       .getOrElse("NoLiteralArguments")(this.config)
+       .map { newConfig => new NoLiteralArguments(newConfig) }
```

The `withConfiguration` method is called once after the rule is loaded. The same
rule instance is then used to process multiple files in the same project.

The final step is to use the configuration to report errors only for literals
types the user has configured to prohibit

```diff
- case t: Lit.Boolean =>
+ case t: Lit if config.isDisabled(t) =>
```

Congrats! The `NoLiteralArguments` linter is now configurable. Putting it
together, the final code looks like this

```scala
// NoLiteralArguments.scala
package fix

import metaconfig.Configured
import scala.meta._
import scalafix.v1._

case class LiteralArgument(literal: Lit) extends Diagnostic {
  override def position: Position = literal.pos
  override def message: String =
    s"Use named arguments for literals such as 'parameterName = $literal'"
}

case class NoLiteralArgumentsConfig(
    disabledLiterals: List[String] = List("Boolean")
) {
  def isDisabled(lit: Lit): Boolean = {
    val kind = lit.productPrefix.stripPrefix("Lit.")
    disabledLiterals.contains(kind)
  }
}

object NoLiteralArgumentsConfig {
  val default = NoLiteralArgumentsConfig()
  implicit val surface =
    metaconfig.generic.deriveSurface[NoLiteralArgumentsConfig]
  implicit val decoder =
    metaconfig.generic.deriveDecoder(default)
}

class NoLiteralArguments(config: NoLiteralArgumentsConfig)
    extends SyntacticRule("NoLiteralArguments") {
  def this() = this(NoLiteralArgumentsConfig.default)
  override def withConfiguration(config: Configuration): Configured[Rule] = {
    config.conf
      .getOrElse("NoLiteralArguments")(this.config)
      .map(newConfig => new NoLiteralArguments(newConfig))
  }
  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree
      .collect {
        case Term.Apply(_, args) =>
          args.collect {
            case t: Lit if config.isDisabled(t) =>
              Patch.lint(LiteralArgument(t))
          }
      }
      .flatten
      .asPatch
  }
}
```

That completes the tutorial in implementing rules. Now let's run the rule on
real-world codebases.

There are two ways to run a custom rule: from source or from pre-compiled
artifacts.

## Run the rule from source code

Running a rule from source code is the simplest way to run a custom rule.
However, rules that are compiled from source have the following limitations:

- Inflexible, rules must be implemented in a single source file
- No dependencies, rules can only use the Scalafix public API
- Slow, rule is re-compiled on every invocation so it's not great for
  interactive usage.
- No tab completion in the sbt shell, users need to manually type the path to
  the source file

The steps below assume you have scalafix setup according to the installation
instructions. The SemanticDB compiler plugin must be enabled to run semantic
rules like `NamedLiteralArguments`. Syntactic rules like the linter
`NoLiteralArguments` work without SemanticDB and don't require a `--classpath`
(when using the command-line interface).

You have different options to run the rule from source: `file:`, `http:` or
`github:`

### Using `file:`

If you have the source code for the rule on your local machine, you can run a
custom rule using the `file:/path/to/NamedLiteralArguments.scala` syntax.

```
scalafix --rules=file:/path/to/NamedLiteralArguments.scala
```

### Using `http:`

Another way to run a rule from source is to publish it as a gist and share the
raw URL

```
scalafix --rules=https://gist.githubusercontent.com/olafurpg/eeccf32f035d13f0728bc94d8ec0a776/raw/78c81bb7f390eb98178dd26ea03c42bd5a998666/NamedLiteralArguments.scala
```

### Using `github:`

Another way to run custom rules from source is to use the `github:org/repo`
scheme.

```
scalafix --rules=github:olafurpg/named-literal-arguments
```

The expansion rules for `github:org/repo` are the following:

| Before                                 | After                                                                    |
| -------------------------------------- | ------------------------------------------------------------------------ |
| `github:org/repo`                      | `scalafix/rules/src/main/scala/fix/Repo.scala`                           |
| `github:org/some-repo`                 | `scalafix/rules/src/main/scala/fix/SomeRepo.scala`                       |
| `github:org/repo/RuleName`             | `scalafix/rules/src/main/scala/fix/RuleName.scala`                       |
| `github:org/repo/RuleName?sha=HASH125` | (at commit `HASH125`) `scalafix/rules/src/main/scala/fix/RuleName.scala` |

## Publish the rule to Maven Central

Run the following sbt command to publish a rule locally.

```sh
> rules/publishLocal
```

To publish a rule online you need additional steps. First, add the settings
below to your `build.sbt` (replacing `ch.epfl.scala`, `scalacenter/scalafix` and
`olafurpg` with your own organization and project name)

```scala
// build.sbt
inThisBuild(List(
  organization := "ch.epfl.scala",
  homepage := Some(url("https://github.com/scalacenter/scalafix")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "olafurpg",
      "Ólafur Páll Geirsson",
      "olafurpg@gmail.com",
      url("https://geirsson.com")
    )
  )
))
```

Next, add the `sbt-ci-release` plugin to `project/plugins.sbt`

```scala
// project/plugins.sbt
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.2.2")
```

If you have not setup GPG on your machine, run the following command

```sh
gpg --gen-key
```

Finally, create a Sonatype account if you have never published to Sonatype
before. Follow the instructions
[here](https://central.sonatype.org/pages/ossrh-guide.html). If you don't have a
domain name, you can use `com.github.@username`. Here is a template you can use
to write the Sonatype issue:

```text
Title:
Publish rights for com.github.olafurpg
Description:
Hi, I would like to publish under the groupId: com.github.olafurpg.
It's my GitHub account https://github.com/olafurpg/
```

Once Sonatype and GPG are setup, run the following command to publish the rule
online.

```sh
> rules/publishSigned
> sonatypeRelease
```

Once published, users can run your rule with the following sbt command.

```sh
// sbt shell
> scalafix dependency:NamedLiteralArguments@com.geirsson:named-literal-arguments:VERSION
```

To permanently install the rule for a build, users can add the dependency to
`build.sbt` by updating `scalafixDependencies in ThisBuild`.

```scala
// build.sbt
scalafixDependencies in ThisBuild +=
  "com.geirsson" %% "named-literal-arguments" % "VERSION"
// sbt shell
> scalafix NamedLiteralArguments
```

For builds using `project/*.scala` files, add the following auto import

```scala
import scalafix.sbt.ScalafixPlugin.autoImport._
```

Users of the Scalafix command-line interface can use the `--tool-classpath` flag

```
scalafix \
  --tool-classpath $(coursier fetch com.geirsson:named-literal-arguments_2.12:VERSION -p) \
  -r NamedLiteralArguments \
  --classpath MY_PROJECT_CLASSPATH \
  my-project/src/main/scala
```

Note that for syntactic rules like `NoLiteralArguments`, the `--classpath`
argument is not required.

### Adding custom resolver

Update the `scalafixResolvers` setting to resolve rules that are published to a
custom repository like Bintray or a private Nexus.

```scala
// build.sbt
import com.geirsson.coursiersmall.{Repository => R}
scalafixResolvers.in(ThisBuild) ++= List(
  R.SonatypeSnapshots,
  R.bintrayRepo("scalacenter", "releases"),
  R.bintrayIvyRepo("sbt", "sbt-plugin-releases"),
  new R.Maven("https://oss.sonatype.org/content/repositories/snapshots/"),
  new R.Ivy(s"https://dl.bintray.com/sbt/sbt-plugin-releases/")
)
```

### Conclusion

Don't be intimidated by publishing to Maven Central, it gets easier once you've
done it the first time. A more detailed guide on how to publish libraries can be
found [here](https://github.com/olafurpg/sbt-ci-release). The benefits of
publishing a rule to Maven Central are many.

- Bring your own dependencies, you can use custom library dependency to
  implement a published rule.
- Fast to run, no need to re-compile the rule on every Scalafix invocation.
- Tab completion in sbt, users can tab-complete your rule when using sbt plugin.
