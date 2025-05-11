# Contributing

Contributions to Scalafix are welcome! This document is a guide to help you get
familiar with the Scalafix build, if you are unsure about anything, don't
hesitate to ask in the [Discord channel](https://discord.gg/8AHaqGx3Qj).

## Modules

### For writing rules

- `scalafix-core/` data structures for rewriting and linting Scala source code.
- `scalafix-rules/` built-in rules such as `RemoveUnused`.

### For executing rules

- `scalafix-cli/` command-line interface.
- `scalafix-reflect/` utilities to compile and classload rules from
  configuration.

### For tool integration
- `scalafix-interfaces/` Java facade to run rules within an existing JVM instance.
- `scalafix-loader/` Java implementation to dynamically fetch and load
  implementations to run rules.
- `scalafix-versions/` Java implementation to advertize which Scala versions
  `scalafix-cli` is published with.

### Others

- `scalafix-tests/` projects for unit and integration tests.
- `scalafix-docs/` documentation code for the Scalafix website.

## IntelliJ import

The project should import normally into IntelliJ and there should not be any
false red squiggles. To use the debugger or run tests from within IntelliJ, run
at least once tests through sbt to generate a `BuildInfo` file and property
files for Scalafix testkit.

## Testing

Start the SBT shell with `$ sbt`. The commands below assume you have a running
sbt shell.

```sh
# Fast unit tests for core, rules, reflect, cli & testkit.
> unit3_3_4 / test

# Integration tests for core, rules, reflect, cli & testkit.
# Contains a lot of different test suites, so it's recommended
# to use testOnly and/or testQuick.
> integration3_3_4 / test

# Use testWindows to exclude tests that are not expected to succeed
# on that OS.
> unit3_3_4 / testWindows
> integration3_3_4 / testWindows

# Only run tests for built-in rules, using scalafix-testkit.
> expect3_3_4Target2_13_14 / test

# Only run ProcedureSyntax unit tests.
> expect3_3_4Target2_13_14 / testOnly -- -z ProcedureSyntax
```

[sbt-projectmatrix](https://github.com/sbt/sbt-projectmatrix) is used to
generate several sbt projects `expectTargetY` with the same source code,
but for a combination of Scala versions:
- used for compiling the framework and rules (`X`)
- used for compiling and generating SemanticDB files for the test input (`Y`)

Unit tests for rules are written using scalafix-testkit

```
scalafix-tests
|
├── unit          # Unit test suites
|
├── integration   # Integration test suites
|
├── shared        # Code that is shared between input and unit projects
├── input         # Source files to be analyzed and fixed by rules
├── output        # Expected output from running rewrite rules
└── expect        # Verify expectations defined in input/output using testkit
```

## Formatting
We use scalafix to apply some rules that are configured in .scalafix.conf.
Make sure to run `sbt "dogfoodScalafixInterfaces; scalafixAll"` to execute
the latest version of those rules.

Be sure to run `scalafmt` (available in the `bin` folder) to ensure code
formatting. `./bin/scalafmt --diff` formats only the files that have changed
from the main branch. You can read more about it at http://scalafmt.org

## Documentation

The scalafix documentation website uses [Docusaurus](https://docusaurus.io/) and
[mdoc](https://github.com/olafurpg/mdoc).

First, make sure you have the [yarn](https://yarnpkg.com/en/) package manager
installed.

Next, start sbt and compile the markdown documentation. The documentation will
re-generate on file save.

```sh
$ sbt
> ci-docs
```

To view the website in your browser, start the Docusaurus live reload server

```sh
cd website
yarn install
yarn start
```

Consult the Docusaurus website for instructions on how to customize the sidebar
or tweak the landing page.

## Binary Compatibility

To avoid breaking binary compatibility we use
[sbt-version-policy](https://github.com/scalacenter/sbt-version-policy).

Anything under the package `scalafix.internal._` does not have compatibility
restrictions.

Run `sbt versionPolicyCheck` to check for any compatibility issues.

## Publish setup

Scalafix uses [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) to
automate Sonatype releases. A new SNAPSHOT release is published on every merge
into main. A stable release is published to Maven Central on every git tag.

## Releasing

First, kickstart a CI release to Sonatype by pushing a git tag that correspond to the desired commit

```sh
git fetch && git log origin/main --pretty=oneline # choose the commit hash you want to tag
COMMIT_HASH=14a069a3765739f5540129e8220104b17f233020 # change this variable
VERSION=0.9.15 # change this variable
git tag -af "v$VERSION" $COMMIT_HASH -m "v$VERSION" && git push -f origin v$VERSION
```

While the CI is running, update the release notes at
https://github.com/scalacenter/scalafix/releases

After the CI completes, confirm that the release has successfully finished

```
./bin/test-release.sh $VERSION
```

You may need to update the test-release.sh script to include new cross-build
artifacts (for example a new Scala version).

Next, open a PR to sbt-scalafix that updates the version here
https://github.com/scalacenter/sbt-scalafix/blob/7a4b51ae520c26ab2b09c9bedb6e3962f809ac53/project/Dependencies.scala#L5.
Once the CI for this change is green, merge and push a new git tag to
sbt-scalafix.

Validate that the sbt-scalafix release completed successfully by running the
test-release.sh script in the sbt-scalafix repository.

When scalafix and sbt-scalafix have both completed the release, edit the release
draft in the GitHub web UI to point to the tag that you pushed and then click on
"Publish release".

Confirm that the documentation is advertising the new versions
- https://scalacenter.github.io/scalafix/docs/users/installation.html#sbt
- https://scalacenter.github.io/scalafix/docs/users/installation.html#help

If everything went smoothly, congrats! Tweet about the release and comment on
Discord linking to the release notes.

If something goes wrong for any reason making the artifacts not reach maven,
delete the pushed tag with the following command

```sh
TAG=??? # for example "v0.5.3"
git tag -d $TAG
git push origin :refs/tags/$TAG
```

It's important that the latest tag always has an accompanying release on Maven.
If there is no release matching the latest tag then the docs will point to
scalafix artifacts that cannot be resolved.
