---
id: before-you-begin
title: Before you write code
---

Before you dive right into the implementation details of your rule, it's good to
ask yourself the following questions first.

## Do you want a linter or rewrite?

- Rewrites can automatically fix problems but can only address problems that
  have unambiguous solutions.
- Linters highlight problematic code without providing a fix, which means they
  require the user to fix the problem. Linters are not limited to problems that
  have unambiguous solutions so linters can address a larger problem space than
  rewrites.

## Is your rule syntactic or semantic?

- Syntactic rules are simple to run because they don't require compiling input
  sources beforehand but syntactic rules can only do limited code analysis
  since they don't have access to compiler information such as symbols and
  types.
- Semantic rules are are slower and more complicated to run since they require
  compilation but on the other hand semantic rules are able to perform more
  advanced code analysis since they have access to compiler information such as
  symbols and types.

## What is an acceptable false-positive rate?

- If you are doing a one-off migration script for your company codebase then it
  might be OK to manually fix trickier corner cases.
- If your linter detects serious production bugs, it might be OK that it has a
  few false positives. If your linter reports low-importance issues like "method
  name should start with lowercase character", then false positives are not
  acceptable.

## What diff does the rewrite produce?

Before implementing a rule, it's good to manually migrate/refactor a few
examples first. Manual refactoring is great to discover corner cases and
estimate how complicated the rule needs to be.

## Who will use your rule?

The target audience/users of your rule can impact the implementation of the
rule.  If you are the only end-user of the rule, then you can maybe take
shortcuts and worry less about rare corner cases that may be easier to fix
manually. If your rule is intended to be used by the entire Scala community,
then you might want to be more careful with corner cases.

## What codebase does your rule target?

Is your rule specific to a particular codebase or is the rule intended to be
used for any arbitrary project? It's easier to validate a rule if it will only
run on a single codebase. You may not even need tests, since the codebase is the
only test. If your rule is intended to be used in any random codebase, you need
to put more effort into handling corner cases. In general, the smaller the
target domain of your rule, the easier it is to implement a rule.

## How often will your rule run?

Are you writing a one-time migration script or will your rule run on every pull
request? A rule that runs on every pull request should ideally have some unit
tests and be documented so that other people can help maintain the rule.
