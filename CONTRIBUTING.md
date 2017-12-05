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

Start an sbt shell with `$ sbt`.
The commands below assume you have a running sbt shell.

```sh
> unit/test # Fast unit tests for rules, cli, core. Contains a lot
            # of different test suites, so it's recommended to use testOnly.
> unit/testOnly scalafix.tests.rule.* # Only run tests for rules, using scalafix-testkit.
> unit/testOnly scalafix.tests.core.* # Only run tests for core APIs.
> unit/testOnly scalafix.tests.cli.*  # Only run tests for the command line interface.

# SBT plugin
# (recommended) start the sbt shell with a SNAPSHOT scalafix version:
# $ sbt -Dscalafix.version=0.5-SNAPSHOT
> scalafix-sbt/it:test  # publishes modules locally and runs scripted (slow).
                        # Only needed once per changed in core/cli modules.
> scalafix-sbt/scripted # only run scripted tests (still slow, but skips
                        # publishLocal for core/cli modules)
```

Unit tests for rules are written using scalafix-testkit, read more about
it here:
https://scalacenter.github.io/scalafix/docs/rule-authors/setup#scalafix-testkit

```
scalafix-tests
├── input         # Source files to be fixed by default rules
├── input-sbt     # Source files to be fixed by Sbt1 rule
├── output        # Expected output from running default rules
├── output-dotty  # Expected output from running Dotty-specific rules
├── output-sbt    # Expected output from running Sbt1 rule
├── shared        # Source files that are compiled semanticdb, used in BaseSemanticTest.
└── unit          # Unit test suites.
```

## Formatting

Be sure to run `scalafmt` (available in the root folder) to ensure code formatting.
`./scalafmt --diff` formats only the files that have changed from the master branch.
You can read more about it at http://scalafmt.org

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

All documentation is written in markdown, using [Liquid templates](https://jekyllrb.com/docs/templates/).

### Side menu
If you add/remove/move a docs (sub)section, you may want to edit the side menu. You'll find it [here](https://github.com/gabro/scalafix/blob/microsite/website/src/main/resources/microsite/data/menu.yml).

### Custom tags
We also have a few custom Liquid tags that you can use.

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

If you need to link to a vocabulary term you can use the `glossary_ref` tag, for example:

```md
{% glossary_ref Patch %}
// Expands to: /scalafix/docs/creating-your-own-rule/vocabulary.html#patch
```

If you need to link to a page in the apidocs you can use the `apidocs_ref` tag, for example:

```md
{% apidocs_ref scalafix.patch.PatchOps %}
// Expands to: /scalafix/docs/api/scalafix/patch/PatchOps.scala
```

## Binary Compatibility

To avoid breaking binary compatiblity we use the Migration Manage for Scala or [Mima](https://github.com/lightbend/migration-manager) for short.

Anything under the package `scalafix.internal._` does not have compatibility restrictions.

Run `sbt mimaReportBinaryIssues` to check for any compatibility issues.

## Publish setup

Scalafix is setup to publish automatically when we publish a tag on GitHub. You should not have to worry
about this part. For completeness, this is how it was set up:

### Register on Sonatype

Follow https://github.com/scalacenter/sbt-release-early/wiki/How-to-release-with-Sonatype#release-with-sonatype

You now have:

* SONATYPE_PASSWORD
* SONATYPE_USERNAME

Ask us to be added to the ch.epfl.scala group

### Setup gpg

```bash
gpg --version # make shure it's 1.X (not 2.X)
gpg --gen-key # with an empty passphrase
gpg --armor --export-secret-keys | base64 -w 0 | xclip -i # This is PGP_SECRET
gpg --list-keys

# For example
# /home/gui/.gnupg/pubring.gpg
# ----------------------------
# pub   2048R/6EBD580D 2017-12-04
# uid                  GMGMGM
# sub   2048R/135A5E9E 2017-12-04

gpg --keyserver hkp://pool.sks-keyservers.net --send-keys <YOUR KEY ID>

# For example: gpg --keyserver hkp://pool.sks-keyservers.net --send-keys 6EBD580D
```

### Setup travis

Setup the project at https://travis-ci.org/scalacenter/scalafix/settings with

SONATYPE_PASSWORD
SONATYPE_USERNAME
PGP_PASSPHRASE (empty)
PGP_SECRET

## TL;DR

If you are unsure about anything, don't hesitate to ask in the [gitter channel](https://gitter.im/scalacenter/scalafix).
