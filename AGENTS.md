# Scalafix AI Coding Agent Instructions

## Project Overview

Scalafix is a refactoring and linting tool for Scala. It supports syntactic and
SemanticDB-based rules and uses sbt 2's built-in project matrix to test and publish
artifacts across Scala compiler versions.

Main modules:
- `scalafix-interfaces`: Java API for embedding Scalafix in a JVM application.
- `scalafix-core`: rewriting, linting, and public rule APIs; its published matrix
  targets the Scala 2 line.
- `scalafix-rules`: built-in rules, cross-built for the supported Scala lines.
- `scalafix-reflect`: runtime compilation/loading support for external rules.
- `scalafix-cli`: command-line application and execution engine.
- `scalafix-testkit`: test framework for rule input/output fixtures.
- `scalafix-tests`: unit, integration, and rule expectation tests.
- `scalafix-docs`: code used to generate documentation.

The build also has standalone `core3` and `reflect3` compile-check projects.
They are not ordinary published matrix rows. In particular, the Scala 3 CLI
uses the Scala-2-built reflect artifact; inspect the project dependency
workaround in `build.sbt` before changing this arrangement.

## Build Architecture

### sbt project matrix

sbt 2's built-in project matrix is used in this build.

Most modules are `ProjectMatrix` definitions in `build.sbt`; they generate
separate sbt projects for the configured Scala compiler versions. Project IDs
are generated from those versions, so do not rely on a remembered concrete ID.
Use `sbt projects` to find the current IDs.

`project/TargetAxis.scala` adds a second dimension for tests:
- `jvmPlatformTargets` creates projects for compiling test input/output with a
  target Scala version and its associated compiler options.
- `jvmPlatformAgainstTargets` creates test projects pairing a framework/rules
  compiler version with an input target version.
- `TargetAxis` can distinguish normal compilation from `-Xsource:3` compilation.
- Resolution prefers an exact compiler-version match and falls back to a binary
  Scala version match while preserving the source-mode axis.

As a result, `unit` and `integration` projects are matrix projects, and rule
expectation projects include both a framework compiler and a target compiler in
their generated ID. The `input` and `output` projects compile fixtures under
target compiler settings; `expect` runs the testkit against those fixtures.
Avoid treating these projects as a conventional single `++<version>` build.

### Version and matrix maintenance

- `project/Dependencies.scala` is the central place for supported Scala lines
  and dependency versions.
- `project/ScalafixBuild.scala` derives matrix versions and target combinations,
  applies runtime-Java compatibility filters, and records previous compiler
  versions used for compatibility checks.
- `project/TargetAxis.scala` defines target lookup and source-mode behavior.
- `build.sbt` defines module dependencies and which projects participate in
  each matrix.

When adding, removing, or updating a Scala compiler line, inspect all of these
places plus version-specific sources/fixtures and CI. Avoid copying a generated
project ID into docs or scripts when it can be derived from the matrix.

## Development Workflows

### Tests

The root `sbt test` command is the aggregate CI test run and can take a while.
For focused work, list the current matrix projects with `sbt projects`, then
scope the task to the appropriate generated project:

```text
# In the sbt shell; replace placeholders with IDs shown by `projects`.
> <unit-project-id> / test
> <integration-project-id> / testOnly -- -z <test-name>
> <expect-project-id> / test
> <integration-project-id> / testWindows
```

Use `unit` for fast unit tests, `integration` for end-to-end tests, and `expect`
for built-in rule input/output expectations. Integration suites are numerous;
prefer `testOnly` (or `testQuick`) when a focused test is available. The
`testWindows` task excludes tests tagged with
`scalafix.internal.tests.utils.SkipWindows`; use it for Windows-compatible
coverage.

For a rule expectation, choose an `expect` project whose compiler/target pair
matches the behavior under test. If a command copied from old docs no longer
resolves, check `sbt projects` and use the current generated project ID rather
than guessing a version suffix.

Running an sbt test at least once also generates BuildInfo and testkit property
files needed to run tests or use the debugger from IntelliJ.

### Dogfooding and formatting

`.scalafix.conf` configures the rules applied to this repository. The
`dogfoodScalafixInterfaces` command publishes local CLI artifacts and their
dependencies, reloads the sbt plugins with the local interfaces artifact, and
returns to the main build. Run it before `scalafixAll` so the latest local rules
are used:

```bash
sbt "dogfoodScalafixInterfaces; scalafixAll"
```

To check the dogfooding and formatting used by CI:

```bash
sbt "dogfoodScalafixInterfaces; scalafixAll --check"
./bin/scalafmt --test
```

`./bin/scalafmt` formats all files; `./bin/scalafmt --diff` formats changed
files.

### Documentation and compatibility

- `sbt ci-docs` compiles the documentation. To preview the site, run
  `cd website && yarn install && yarn start`.
- `sbt versionPolicyCheck` checks binary-compatibility policy. Public APIs under
  `scalafix.v1` follow Early SemVer; `scalafix.internal` packages have no
  compatibility guarantee.
- `sbt save-expect` regenerates integration `.expect` snapshots. Review its
  changes; do not use it as a substitute for checking an unexpected diff.

## Rule Development

### Rule kinds and implementation

- A `SyntacticRule` uses the parsed syntax tree and does not require SemanticDB.
- A `SemanticRule` uses a `SemanticDocument` for symbols and semantic
  information. SemanticDB must be generated for the input code.

Built-in rule implementations are under
`scalafix-rules/src/main/scala/scalafix/internal/rule/` (with version-specific
source directories where necessary). When adding a built-in rule:
1. Follow the relevant `scalafix.v1.Rule`, `SyntacticRule`, or `SemanticRule`
   pattern in `scalafix-core/src/main/scala/scalafix/v1/Rule.scala`.
2. Register it in
   `scalafix-rules/src/main/resources/META-INF/services/scalafix.v1.Rule`.
3. Return `Patch` operations rather than constructing whole-file diffs.
4. Group related edits with `.atomic` when they must be suppressed together.
5. Add or update rule fixtures and run the matching `expect` project.

Useful APIs include `Patch.addLeft`, `Patch.addRight`, `Patch.removeToken`,
`Patch.replaceTree`, `tree.collect`, `tree.symbol`, and `SymbolMatcher`.
Prefer existing rule implementations and
`docs/developers/tutorial.md` for idioms and API details.

### Rule test fixtures

The testkit fixtures live under `scalafix-tests/`:
- `input/`: source code to analyze or rewrite.
- `output/`: expected rewritten source; keep paths paired with `input/`.
- `expect/`: test suite that runs the rule expectations.
- `unit/` and `integration/`: ordinary unit and integration test suites.

Input fixtures can select rules with a leading configuration comment and assert
lint diagnostics inline (replace `LintID` with the expected diagnostic ID), for
example:

```scala
/* rules = MyRule */
someCode // assert: LintID
```

Preserve the expected-output pairing and any assertion comments when changing
fixtures. Some fixture directories are compiler-version-specific; check the
matching source directories before adding a common or version-specific test.

## Cross-Version and Module Conventions

- Use `CrossVersion.for3Use2_13` when Scala 3 code must consume a Scala 2.13
  artifact, and follow nearby dependency exclusions to avoid duplicate binary
  artifacts.
- The build's `resolve` helpers select a compatible project from a matrix.
  Use `TargetAxis.resolve` when the requested target must also match its
  `TargetAxis` settings (including `-Xsource:3`).
- The module graph is defined in `build.sbt`; check it before adding a dependency
  or assuming one module is available on every Scala line.
- Shared compiler-specific code belongs in the appropriate `scala-*` source
  directory rather than being duplicated in the common source tree.

## Key Files

- `build.sbt`: module definitions, matrix rows, and module dependencies.
- `project/ScalafixBuild.scala`: shared sbt settings, commands, and version
  compatibility logic.
- `project/TargetAxis.scala`: test target axes and matrix resolution.
- `project/Dependencies.scala`: compiler and library version declarations.
- `scalafix-core/src/main/scala/scalafix/v1/Rule.scala`: public rule APIs.
- `scalafix-testkit/src/main/scala/scalafix/testkit/SemanticRuleSuite.scala`:
  semantic rule test framework.
- `docs/developers/tutorial.md`: rule-development tutorial.
- `CONTRIBUTING.md`: contributor workflows and project/module overview.
