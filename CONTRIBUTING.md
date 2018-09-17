# Contributing

Contributions to Scalafix are welcome! This document is a guide to help you get
familiar with the Scalafix build, if you are unsure about anything, don't
hesitate to ask in the [gitter channel](https://gitter.im/scalacenter/scalafix).

## Modules

- `scalafix-core/` data structures for rewriting and linting Scala source code
- `scalafix-reflect/` JVM-only utilities to compile and classload custom rules
- `scalafix-rules/` Scalafix built-in rules such as `RemoveUnused`
- `scalafix-cli/` command-line interface
- `scalafix-tests/` project for running unit and integration tests.
- `readme` documentation page

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

Unit tests for rules are written using scalafix-testkit, read more about it
here: https://scalacenter.github.io/scalafix/docs/developers/setup

```
scalafix-tests
├── input         # Source files to be fixed by default rules
├── output        # Expected output from running default rules
├── shared        # Source files that are compiled semanticdb, used in BaseSemanticTest.
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

- [ ] Releases > "Draft a new release"
- [ ] Write changelog, linking to each merged PR and attributing contributor,
      following a similar format as previous release notes.
- [ ] "Publish release", this pushes a tag and triggers the CI to release to
      sonatype.
- [ ] after CI releases, double check the end of logs of the entry where
      CI_PUBLISH=true. You have to expand the after_success section.
- [ ] after sonatype release is completed, double check after ~30 minutes that
      the artifacts have synced to maven by running this command:

          coursier fetch ch.epfl.scala:scalafix-core_2.12:VERSION

- [ ] once the artifacts are synced to maven, go to the scalafix repo and update
      the `scalafix` binary with the following command and open a PR to the
      scalafix repo.

         coursier bootstrap ch.epfl.scala:scalafix-cli_2.12.4:VERSION -f --main scalafix.cli.Cli -o scalafix -f

If everything went smoothly, congrats!

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
