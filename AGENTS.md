# Scalafix AI Coding Agent Instructions

## Project Overview

Scalafix is a refactoring and linting tool for Scala. It enables syntactic and semantic source code transformations through a rule-based system. The project targets multiple Scala versions (2.12, 2.13, 3.3-3.8) and is built using a complex sbt-projectmatrix setup.

**Core Architecture:**
- `scalafix-interfaces`: Java API for JVM integration (cross-platform, no Scala dependency)
- `scalafix-core`: Data structures for AST rewriting and linting (Scala 2.12/2.13)
- `scalafix-rules`: Built-in rules like `RemoveUnused`, `ProcedureSyntax` (cross-compiled for all Scala versions)
- `scalafix-reflect`: Runtime rule compilation (Scala 2 only)
- `scalafix-cli`: Command-line interface (cross-compiled for all Scala versions)
- `scalafix-testkit`: Testing framework for rules using input/output file pairs

## Critical Build Architecture

### sbt-projectmatrix Cross-Building
The build uses `sbt-projectmatrix` to generate multiple sub-projects per module, cross-compiled against different Scala versions. This is NOT standard Scala cross-building.

**TargetAxis System**: Custom mechanism in `project/TargetAxis.scala` that creates test matrix combinations:
- Each test project is built with a specific Scala version (the "target")
- Test frameworks compiled with another Scala version can test against that target
- Some targets include `-Xsource:3` flag (tagged with `Xsource3Axis`)
- Example: `expect3_3_6Target3_3_6` tests rules compiled with 3.3.6 against input also compiled with 3.3.6

**Key build concepts:**
- `projectMatrix`: Defines modules that cross-compile
- `.jvmPlatform(scalaVersions)`: Creates one sub-project per Scala version
- `.jvmPlatformTargets(targets)`: Creates sub-projects for test targets
- `.jvmPlatformAgainstTargets(tuples)`: Creates test projects that run with one Scala version against target compiled with another
- Use `resolve(matrix, key)` to lookup and extract settings from the matching sub-project

## Development Workflows

### Running Tests
```bash
# Unit tests for latest Scala 3.3.6 version
sbt "unit3_3_6 / test"

# Integration tests (contains many suites - use testOnly to narrow)
sbt "integration3_3_6 / testOnly -- -z ProcedureSyntax"

# Test built-in rules using scalafix-testkit
sbt "expect3_3_6Target3_3_6 / test"

# Windows-compatible tests only
sbt "unit3_3_6 / testWindows"
```

**Pattern**: Test project names combine the Scala version used to compile the test framework with the target being tested: `unit{CompilerVersion}` or `expect{CompilerVersion}Target{TargetVersion}`

### Applying Scalafix to Itself (Dogfooding)
```bash
# First, publish local artifacts for all Scala versions
sbt "dogfoodScalafixInterfaces; scalafixAll"
```

The `dogfoodScalafixInterfaces` command:
1. Runs `publishLocalTransitive` on all CLI versions
2. Reloads sbt plugins
3. Overrides `scalafix-interfaces` version in meta-build
4. Returns to main build

### Formatting
```bash
# Format all files
./bin/scalafmt

# Format only changed files
./bin/scalafmt --diff
```

### Documentation
```bash
# Generate docs with mdoc and start Docusaurus server
sbt ci-docs
cd website && yarn start
```

## Rule Development Patterns

### Rule Types
- **SyntacticRule**: Works with AST only, no semantic information (fast)
  - Example: `ProcedureSyntax` - replaces `def foo { }` with `def foo: Unit = { }`
- **SemanticRule**: Requires SemanticDB (compiler symbols, types)
  - Example: `RemoveUnused` - removes imports/terms flagged by `-Wunused`
  - Requires `-Wunused`, `-Ywarn-unused`, or `-Wall` compiler flag

### Creating Rules
Rules extend `scalafix.v1.Rule` and must:
1. Define in `scalafix-rules/src/main/scala/scalafix/internal/rule/`
2. Register in `src/main/resources/META-INF/services/scalafix.v1.Rule`
3. Return `Patch` operations (not string diffs)
4. Use `.atomic` on patches for suppression support

**Key APIs:**
- `Patch.addLeft(tree, string)` / `Patch.addRight(tree, string)`: Insert text
- `Patch.removeToken(token)`: Delete token
- `Patch.replaceTree(tree, string)`: Replace tree node
- `tree.symbol`: Get symbol from tree (requires `SemanticDocument`)
- `SymbolMatcher`: Pattern match symbols like `val HasOption = SymbolMatcher.normalized("scala/Option#")`
- `tree.collect { case ... }`: Traverse AST

### Testing Rules with Testkit
Test structure in `scalafix-tests/`:
- `input/`: Source files to transform (with special comments for assertions)
- `output/`: Expected output after rewrite
- `expect/`: Test suite that compares input→output transformation

Special comment syntax in input files:
```scala
/* rules = MyRule */  // Configure which rules to run
import Unused  // assert: UnusedImport  (linter assertion)
```

## Project-Specific Conventions

### Cross-Version Compatibility
- Use `CrossVersion.for3Use2_13` for Scala 3 depending on Scala 2.13 artifacts
- Scala 3 rules depend on Scala 2.13 compiled `scalafix-core` (no native Scala 3 core yet)
- Use `semanticdbScalacCore.cross(CrossVersion.full)` for compiler plugins

### Module Dependencies
- `rules` depends on `core` (rewriting primitives)
- `cli` depends on `rules` + `reflect` (to load external rules)
- `testkit` depends on `cli` (full execution environment)
- `reflect` only exists for Scala 2 (no Scala 3 version)

### Binary Compatibility
- Use `sbt versionPolicyCheck` before releasing
- `scalafix.internal._` packages have no compatibility guarantees
- Public API in `scalafix.v1` follows Early SemVer

### Configuration Files
- `.scalafix.conf`: Rules applied when dogfooding (uses HOCON format)
- Rules configured via `withConfiguration()` method
- Test files use `/* rules = ... */` comments for configuration

## Common Pitfalls

1. **Wrong test scope**: Use `unit3_3_6 / test` not `test` (ambiguous without projectmatrix context)
2. **Missing BuildInfo**: IntelliJ debugger requires running `sbt "unit3_3_6 / test"` once to generate BuildInfo
3. **Scala 3 reflect**: No `reflect3` module - Scala 3 rules can't dynamically compile
4. **Dependency resolution**: When bumping Scala versions, update `versionPolicyIgnored` in `ScalafixBuild.scala`
5. **Test failures on Windows**: Use `testWindows` task, not `test`

## Key Files to Reference

- `project/TargetAxis.scala`: Understand test matrix cross-building
- `project/ScalafixBuild.scala`: Custom sbt settings and commands
- `project/Dependencies.scala`: Scala version constants
- `scalafix-core/src/main/scala/scalafix/v1/Rule.scala`: Rule base classes
- `scalafix-testkit/src/main/scala/scalafix/testkit/SemanticRuleSuite.scala`: Test framework
- `docs/developers/tutorial.md`: Step-by-step rule creation guide
