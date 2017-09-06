Contributing
===========

## Modules

- `scalafix-core/` data structures and algorithms for scalafix rules (linters/checkers/rewrites/fixes)
- `scalafix-cli/` command-line interface
- `scalafix-sbt/` SBT plugin
- `scalafix-nsc/` Scalafix compiler plugin for scalac (codename `nsc` stands for "new Scala compiler")
- `scalafix-tests/` project for running unit and integration tests.
- `readme` documentation page

## Setting up an IDE

I use IntelliJ myself but run all tests from the SBT console.
I've tried to make the project compile from IntelliJ IDEA.
If you experience any issues, don't hesitate to ask on Gitter.

## Testing

```sh
# For rules.
$ sbt scalafix-tests/test # Fast unit tests for rules with access to semantic api.
                          # See core/src/main/resources/ExplicitImplicit for
                          # how to write more unit tests.
                          # You can prefix a test name with "ONLY" to only run
                          # that single test.
$ sbt scalafix-core/test  # Fast unit tests without access to semantic api.
                          # I recommend you use scalafix-nsc/test for rule tests.
                          # For anything but rules, put the tests in core.
$ sbt scalafix-tests/it:test # Slow integration tests running rules on
                             # open source projects.

# For SBT plugin
$ sbt scalafix-sbt/test     # publishes modules locally and runs scripted (slow)
$ sbt scalafix-sbt/scripted # only run scripted tests (still slow, but skips
                            # publishLocal for core/cli/nsc scalafix modules)
```

## Documentation

The scalafix documentation uses [scalatex](http://www.lihaoyi.com/Scalatex/).

To build the docs:

```sh
sbt "~readme/run"
```

To view the docs:

```sh
open readme/target/scalatex/index.html
# OR, for automatic reload on every change
browser-sync start --server --files "readme/target/scalatex/**"
open http://localhost:3000/readme/target/scalatex/
```

## TL;DR

If you are unsure about anything, don't hesitate to ask in the [gitter channel](https://gitter.im/scalacenter/scalafix).
