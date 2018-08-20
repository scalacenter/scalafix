# Contributing

## Modules

- `scalafix-core/` data structures and algorithms for scalafix rules
- `scalafix-reflect/` JVM-only utilities to compile and classload custom rules
- `scalafix-cli/` command-line interface
- `scalafix-tests/` project for running unit and integration tests.
- `readme` documentation page

## Setting up an IDE

I use IntelliJ myself but run all tests from the SBT console. I've tried to make
the project compile from IntelliJ IDEA. If you experience any issues, don't
hesitate to ask on Gitter.

## Testing

Start an sbt shell with `$ sbt`. The commands below assume you have a running
sbt shell.

```sh
> unit/test # Fast unit tests for rules, cli, core. Contains a lot
            # of different test suites, so it's recommended to use testOnly.
> unit/testOnly scalafix.tests.rule.* # Only run tests for rules, using scalafix-testkit.
> unit/testOnly scalafix.tests.core.* # Only run tests for core APIs.
> unit/testOnly scalafix.tests.cli.*  # Only run tests for the command line interface.
```

Unit tests for rules are written using scalafix-testkit, read more about it
here:
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

Scalafix is setup to publish automatically when we publish a tag on GitHub. You
should not have to worry about this part. For completeness, this is how it was
set up:

### Register on Sonatype

Follow
https://github.com/scalacenter/sbt-release-early/wiki/How-to-release-with-Sonatype#release-with-sonatype

You now have:

- SONATYPE_PASSWORD
- SONATYPE_USERNAME

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

SONATYPE_PASSWORD SONATYPE_USERNAME PGP_PASSPHRASE (empty) PGP_SECRET

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

- [ ] update the scalafix version in this build
      [project/plugins.sbt](project/plugins.sbt)

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

## TL;DR

If you are unsure about anything, don't hesitate to ask in the
[gitter channel](https://gitter.im/scalacenter/scalafix).
