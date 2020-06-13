# Contributing

Contributions to Scalafix are welcome! This document is a guide to help you get
familiar with the Scalafix build, if you are unsure about anything, don't
hesitate to ask in the [gitter channel](https://gitter.im/scalacenter/scalafix).

## Modules

- `scalafix-interfaces` Java facade to run rules within an existing JVM instance.
- `scalafix-core/` data structures for rewriting and linting Scala source code.
- `scalafix-reflect/` utilities to compile and classload rules from
  configuration.
- `scalafix-rules/` built-in rules such as `RemoveUnused`.
- `scalafix-cli/` command-line interface.
- `scalafix-tests/` projects for unit and integration tests.
- `scalafix-docs/` documentation code for the Scalafix website.

## IntelliJ import

The project should import normally into IntelliJ and there should not be any
false red squiggles. To use the debugger or run tests from withing IntelliJ, run
at least once `sbt unit/test` to generate a `BuildInfo` file and property files
for Scalafix testkit.

## Testing

Start an sbt shell with `$ sbt`. The commands below assume you have a running
sbt shell.

```sh
> unit/test # Fast unit tests for rules, cli, core. Contains a lot
            # of different test suites, so it's recommended to use testOnly.
> unit/testOnly *RuleSuite # Only run tests for rules, using scalafix-testkit.
> unit/testOnly *RuleSuite -- -z ProcedureSyntax # Only run only ProcedureSyntax unit test.
```

Unit tests for rules are written using scalafix-testkit

```
scalafix-tests
├── input         # Source files to be analyzed and fixed by rules
├── output        # Expected output from running rewrite rules
├── shared        # Code that is shared between input and unit projects
└── unit          # Unit test suites.
```

## Formatting

Be sure to run `scalafmt` (available in the root folder) to ensure code
formatting. `./scalafmt --diff` formats only the files that have changed from
the master branch. You can read more about it at http://scalafmt.org

## Documentation

The scalafix documentation website uses [Docusaurus](https://docusaurus.io/) and
[mdoc](https://github.com/olafurpg/mdoc).

First, make sure you have the [yarn](https://yarnpkg.com/en/) package manager
installed installed.

Next, start sbt and compile the markdown documentation. The documentation will
re-generate on file save.

```sh
$ sbt
> docs/run -w
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

To avoid breaking binary compatiblity we use the Migration Manage for Scala or
[Mima](https://github.com/lightbend/migration-manager) for short.

Anything under the package `scalafix.internal._` does not have compatibility
restrictions.

Run `sbt mimaReportBinaryIssues` to check for any compatibility issues.

## Publish setup

Scalafix uses [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) to
automate Sonatype releases. A new SNAPSHOT release is published on every merge
into master. A stable release is published to Maven Central on every git tag.

### AppVeyor

The cache is limited to 1GB. It's not possible to delete the cache via the ui:
https://github.com/appveyor/ci/issues/985 To delete via curl, get your token at
https://ci.appveyor.com/api-token

```bash
export APPVEYOR_TOKEN="<your-api-token>"
curl -vvv -H "Authorization: Bearer $APPVEYOR_TOKEN" -XDELETE https://ci.appveyor.com/api/projects/scalacenter/scalafix/buildcache
```

## Releasing

First, kickstart a CI release to Sonatype by pushing a git tag that correspond to the desired commit

```sh
git fetch && git log origin/master --pretty=oneline # choose the commit hash you want to tag
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

If everything went smoothly, congrats! Tweet about the release and comment with
`@/all` on Gitter linking to the release notes.

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
