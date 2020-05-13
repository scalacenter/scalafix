---
id: sharing-rules
title: Sharing rules
---

## Run the rule from source code

Running a rule from source code is the simplest way to run a custom rule.
However, rules that are compiled from source have the following limitations:

- Limited code re-use, rule must be implemented in a single source file
- No dependencies, rules can only use the Scalafix public API
- Slow, rule is re-compiled on every invocation so it's not great for
  interactive usage.
- No tab completion in the sbt shell, users need to manually type the path to
  the source file to discover the rule

To run a custom rule from source, start by installing the sbt plugin as
instructed in [here](../users/installation.md#sbt). The SemanticDB compiler
plugin must be enabled to run semantic rules like `NamedLiteralArguments`.

Next, open an sbt shell

```
sbt
>
```

You have different options to run the rule from soruce: `file:`, `http:` or
`github:`

### Using `file:`

The easiest way to run a custom rule from source code is to use the
`file:/path/to/NamedLiteralArguments.scala` syntax.

```
scalafix file:/path/to/NamedLiteralArguments.scala
```

### Using `http:`

Another way to run a rule from source is to publish it as a gist and share the
raw URL

```
scalafix https://gist.githubusercontent.com/olafurpg/eeccf32f035d13f0728bc94d8ec0a776/raw/78c81bb7f390eb98178dd26ea03c42bd5a998666/NamedLiteralArguments.scala
```

### Using `github:`

Another way to run custom rules from source is to use the `github:org/repo`
scheme.

```
sbt
> scalafix github:olafurpg/named-literal-arguments
...
```

The expansion rules for `github:org/repo` is the following

| Before                                 | After                                                                    |
| -------------------------------------- | ------------------------------------------------------------------------ |
| `github:org/repo`                      | `scalafix/rules/src/main/scala/fix/Repo.scala`                           |
| `github:org/some-repo`                 | `scalafix/rules/src/main/scala/fix/SomeRepo.scala`                       |
| `github:org/repo/RuleName`             | `scalafix/rules/src/main/scala/fix/RuleName.scala`                       |
| `github:org/repo/RuleName?sha=HASH125` | (at commit `HASH125`) `scalafix/rules/src/main/scala/fix/RuleName.scala` |

## Publish your rule to Maven Central

Branch:
[`step-6`](https://github.com/olafurpg/named-literal-arguments/commit/88f18b16c9dd939a3f1c08672b121ac2bc1c590d)

The most robust way to share a custom rule is to publish it as a library to
Maven Central. The diff in `step-6` shows the necessary changes to build.sbt to
populate publishing information. The
[sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) readme documents
the further steps to configure gpg and Sonatype.

Once you have your rule, users can depend on it in the sbt plugin by updating
`scalafixDependencies` (`scalafix.sbt.ScalafixPlugin.autoImport.scalafixDependencies`)

```scala
// build.sbt
scalafixDependencies in ThisBuild +=
  "com.geirsson" %% "named-literal-arguments" % "VERSION"
// sbt shell
> scalafix NamedLiteralArguments
```

Users of the Scalafix command-line interface can use the `--tool-classpath` flag

```
scalafix \
  --tool-classpath $(cs fetch com.geirsson:named-literal-arguments_2.12:VERSION) \
  -r NamedLiteralArguments \
  --classpath MY_PROJECT_CLASSPATH \
  my-project/src/main/scala
```

Don't be intimidating by publishing to Maven Central, it gets easier once you've
done it the first time. The benefits of publishing a rule to Maven Central are
many.

- Dependencies, you can use custom library dependency to implement your rule
- Fast to run, no need to re-compile the rule on every Scalafix invocation
- Tab completion in sbt, users can tab-complete your rule when using sbt plugin
  j
