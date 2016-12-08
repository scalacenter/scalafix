Contributing
===========

## Modules

- `core/` the rewrite rules
- `scalafix-sbt/` SBT plugin
- `scalafix-nsc/` Scalafix compiler plugin for scalac (codename `nsc` stands for "new Scala compiler")
- `scalafix-tests/` testing project to run large (and slow) property+integration tests.

## Setting up an IDE

I use IntelliJ myself but run all tests from the SBT console.
I've tried to make the project compile from IntelliJ IDEA.
If you experience any issues, don't hesitate to ask on Gitter.

## Testing

```sh
# For rewrites.
$ sbt scalafix-nsc/test # Fast unit tests for rewrites with access to semantic api.
                        # See core/src/main/resources/ExplicitImplicit for
                        # how to write more unit tests.
                        # You can prefix a test name with "ONLY" to only run
                        # that single test.
$ sbt core/test         # Fast unit tests without access to semantic api.
                        # I recommend you use scalafix-nsc/test for rewrite tests.
                        # For anything but rewrites, put the tests in core.
                        
# For SBT plugin
# Note. "very" prefix is necessary since that publishes both 2.11 and 2.12 versions
# of the compiler plugin.
$ sbt "very publishLocal" # Required for every change in core OR scalafix-nsc.
$ sbt scripted            # For change in scalafix-sbt project.
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
