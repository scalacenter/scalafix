Contributing
===========

## Modules

- `scalafix-core/` data structures and algorithms for scalafix rules.
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
$ sbt scalafix-sbt/it:test  # publishes modules locally and runs scripted (slow)
                            # for hassle-free execution run with version specified
                            # e.g. -Dscalafix.version=0.5-SNAPSHOT
$ sbt scalafix-sbt/scripted # only run scripted tests (still slow, but skips
                            # publishLocal for core/cli/nsc scalafix modules)
```

## Documentation

The scalafix documentation website uses [sbt-microsite](https://47deg.github.io/sbt-microsites/).

You'll need Jekyll to build the website. You can follow the installation instructions
[here](https://jekyllrb.com/docs/installation/).

Once you have Jekyll, build the website with:

```sh
sbt website/makeMicrosite
```

To view the website in your browser:

```sh
cd website/target/site
jekyll serve
open http://localhost:4000/scalafix/
```

All documentation is written in markdown, using [Liquid templates](https://jekyllrb.com/docs/templates/). We also have a few custom Liquid tags that you can use.

If you need to link to a page in the docs, you can use the `doc_ref` tag, for example:

```c
{% doc_ref Before your begin %}
// Expands to: /scalafix/docs/creating-your-own-rule/before-you-begin.html
```

You can also link to a specific section of the page:

```c
{% doc_ref Before your begin, What diff do you want to make %}
// Expands to: /scalafix/docs/creating-your-own-rule/before-you-begin.html#what-diff-you-want-to-make
```

If you need to link to a vocabulary term you can use the `vocabulary_ref` tag, for example:

```md
{% vocabulary_ref Patch %}
```

## TL;DR

If you are unsure about anything, don't hesitate to ask in the [gitter channel](https://gitter.im/scalacenter/scalafix).
